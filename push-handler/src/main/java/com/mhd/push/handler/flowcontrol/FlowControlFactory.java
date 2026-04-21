package com.mhd.push.handler.flowcontrol;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mhd.push.common.constant.CommonConstant;
import com.mhd.push.common.constant.RedisConstant;
import com.mhd.push.common.domain.TaskInfo;
import com.mhd.push.common.dto.account.DingDingRobotAccount;
import com.mhd.push.common.dto.account.EnterpriseWeChatRobotAccount;
import com.mhd.push.common.dto.account.FeiShuRobotAccount;
import com.mhd.push.handler.utils.AccountUtils;
import com.mhd.push.support.domain.entity.ChannelAccount;
import com.mhd.push.support.service.ConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 生产级限流工厂 (入口类)。
 * <p>
 * 设计原则：
 * 1. 统一落到 Redis，保证多实例共享额度（去除了单机内存限流）。
 * 2. 职责分离：规则解析、算法执行、Key生成逻辑清晰。
 * 3. 配置优先级：配置中心 > 本地配置 > 代码兜底。
 *
 * @author zhao-hao-dong
 */
@Service
@Slf4j
public class FlowControlFactory {
    private static final String FLOW_CONTROL_KEY = "flowControlRule";
    private static final String FLOW_CONTROL_PREFIX = "flow_control_";
    @Resource
    private ConfigService configService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private AccountUtils accountUtils;
    private DefaultRedisScript<Long> leakyBucketRedisScript;

    @PostConstruct
    public void init() {
        leakyBucketRedisScript = new DefaultRedisScript<>();
        leakyBucketRedisScript.setResultType(Long.class);
        leakyBucketRedisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("leaky-bucket.lua")));
    }

    /**
     * 执行本次发送任务的限流控制。
     * <p>
     * 流程：解析规则 -> 确认配额 -> 生成Key -> 执行算法 -> 处理等待
     *
     * @param taskInfo          发送任务信息
     * @param flowControlParam  限流参数（可选，用于覆盖默认值）
     */
    public void flowControl(TaskInfo taskInfo, FlowControlParam flowControlParam) {
        // 1. 解析生效规则
        FlowControlRule rule = resolveRule(taskInfo, flowControlParam);
        if (rule == null) return;

        // 2. 计算本次请求消耗的令牌数（通常为1，如果是按用户数限流则为接收人数）
        int permits = resolvePermits(taskInfo, flowControlParam);

        // 3. 生成限流Key（基于规则的作用域）
        String rateKey = buildRateKey(taskInfo, rule);

        // 4. 检查并执行“退避策略” (Backoff)
        // 如果第三方服务之前返回了 429 (Too Many Requests)，这里会强制等待
        long waitMs = waitBackoffIfNeeded(rateKey);

        // 5. 执行“流速”限流 (QPS控制)
        // 根据规则配置的算法，选择漏桶或固定窗口
        if (rule.getQps() != null && rule.getQps() > 0) {
            if (rule.getAlgorithm() == FlowControlAlgorithm.LEAKY_BUCKET) {
                waitMs += applyLeakyBucket(rateKey, rule.getQps(), permits);
            } else {
                // 默认使用固定窗口（代码中 applySecondWindow 实际是固定窗口）
                waitMs += applySecondWindow(rateKey, rule.getQps(), permits);
            }
        }

        // 6. 执行“配额”限流 (总量控制)
        // 检查分钟级和日级配额
        applyQuota(rateKey, "minute", rule.getPerMinute(), permits);
        applyQuota(rateKey, "day", rule.getPerDay(), permits);

        // 7. 日志记录
        if (waitMs > 0) {
            log.info("FlowControl: Wait {} ms, Key:{}, Rule:{}", waitMs, rateKey, JSON.toJSONString(rule));
        }
    }

    /**
     * 记录第三方服务的退避时间。
     * 当第三方明确返回 429 / retry-after 时调用，将退避信息写入 Redis。
     */
    public void recordBackoff(TaskInfo taskInfo, FlowControlParam flowControlParam, long retryAfterMs) {
        if (retryAfterMs <= 0) return;

        FlowControlRule rule = resolveRule(taskInfo, flowControlParam);
        if (rule == null) return;

        String key = RedisConstant.buildFlowControlBackoffKey(buildRateKey(taskInfo, rule));
        // 设置过期时间，值为“过期时间戳”
        stringRedisTemplate.opsForValue().set(key, String.valueOf(System.currentTimeMillis() + retryAfterMs),
                retryAfterMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 等待退避时间。
     *
     * @return 实际等待的毫秒数
     */
    private long waitBackoffIfNeeded(String rateKey) {
        String backoffKey = RedisConstant.buildFlowControlBackoffKey(rateKey);
        Long expire = stringRedisTemplate.getExpire(backoffKey, TimeUnit.MILLISECONDS);

        if (expire == null || expire <= 0) {
            return 0;
        }

        sleep(expire);
        return expire;
    }

    /**
     * 解析最终生效的限流规则。
     * 优先级：配置中心 > handler 默认值
     */
    private FlowControlRule resolveRule(TaskInfo taskInfo, FlowControlParam flowControlParam) {
        String flowControlConfig = configService.getProperty(FLOW_CONTROL_KEY, CommonConstant.EMPTY_JSON_OBJECT);
        JSONObject jsonObject = JSON.parseObject(flowControlConfig);

        // 1. 尝试从配置中心获取精确规则
        FlowControlRule configured = getConfiguredRule(jsonObject, taskInfo);
        if (configured != null) {
            // 补全默认值
            if (configured.getAlgorithm() == null) configured.setAlgorithm(FlowControlAlgorithm.TOKEN_BUCKET);
            if (configured.getScope() == null) configured.setScope(FlowControlScope.CHANNEL_ACCOUNT);
            return configured;
        }

        // 2. 使用参数中的默认值兜底
        if (flowControlParam == null || flowControlParam.getRateInitValue() == null || flowControlParam.getRateInitValue() <= 0) {
            return null;
        }

        return FlowControlRule.builder()
                .algorithm(FlowControlAlgorithm.TOKEN_BUCKET)
                .scope(FlowControlScope.CHANNEL_ACCOUNT)
                .qps(flowControlParam.getRateInitValue())
                .build();
    }

    /**
     * 根据 TaskInfo 构建候选 Key 列表，按优先级从高到低查询配置。
     * 顺序：精确端点 > 租户 > 账号 > 渠道 > 旧版Key
     */
    private List<String> candidateConfigKeys(TaskInfo taskInfo) {
        FlowControlRule directRule = FlowControlRule.builder().scope(FlowControlScope.ACCOUNT_ENDPOINT).build();
        FlowControlRule tenantRule = FlowControlRule.builder().scope(FlowControlScope.CHANNEL_ACCOUNT_TENANT).build();
        FlowControlRule accountRule = FlowControlRule.builder().scope(FlowControlScope.CHANNEL_ACCOUNT).build();
        FlowControlRule channelRule = FlowControlRule.builder().scope(FlowControlScope.CHANNEL).build();

        String directKey = buildRateKey(taskInfo, directRule);
        String tenantKey = buildRateKey(taskInfo, tenantRule);
        String accountKey = buildRateKey(taskInfo, accountRule);
        String channelKey = buildRateKey(taskInfo, channelRule);

        // 旧版兼容 Key
        String legacyChannel = FLOW_CONTROL_PREFIX + taskInfo.getSendChannel();
        String legacyAccount = FLOW_CONTROL_PREFIX + taskInfo.getSendChannel() + "_" + taskInfo.getSendAccount();
        String legacyTenant = FLOW_CONTROL_PREFIX + taskInfo.getSendChannel() + "_" + taskInfo.getSendAccount() + "_" + normalize(taskInfo.getBusinessOwner());

        return List.of(directKey, tenantKey, accountKey, channelKey, legacyTenant, legacyAccount, legacyChannel);
    }

    /**
     * 根据 Key 从配置 JSON 中提取规则。
     */
    private FlowControlRule getConfiguredRule(JSONObject jsonObject, TaskInfo taskInfo) {
        if (jsonObject == null || taskInfo == null) return null;

        for (String key : candidateConfigKeys(taskInfo)) {
            Object value = jsonObject.get(key);
            if (value == null) continue;

            if (value instanceof Number) {
                // 兼容旧版简单配置：只配置了 QPS 数字
                return FlowControlRule.builder()
                        .algorithm(FlowControlAlgorithm.TOKEN_BUCKET)
                        .scope(key.startsWith(FLOW_CONTROL_PREFIX) ? FlowControlScope.CHANNEL : FlowControlScope.CHANNEL_ACCOUNT)
                        .qps(((Number) value).doubleValue())
                        .build();
            }
            // 新版复杂配置：JSON 对象
            return JSON.parseObject(JSON.toJSONString(value), FlowControlRule.class);
        }
        return null;
    }

    private int resolvePermits(TaskInfo taskInfo, FlowControlParam flowControlParam) {
        if (flowControlParam == null || flowControlParam.getRateLimitStrategy() != RateLimitStrategy.SEND_USER_NUM_RATE_LIMIT) {
            return 1;
        }
        return taskInfo.getReceiver() == null || taskInfo.getReceiver().isEmpty() ? 1 : taskInfo.getReceiver().size();
    }

    /**
     * 漏桶：强调匀速输出，适合支付等对“瞬时尖峰”更敏感的通道。
     * 应用漏桶算法，返回需要等待的时间
     *
     * @param rateKey 限流的业务键
     * @param qps     每秒允许的请求数 (Queries Per Second)
     * @param permits 本次请求需要的配额数量
     * @return 需要等待的毫秒数，如果为 0 则表示可以立即执行
     */
    private long applyLeakyBucket(String rateKey, double qps, int permits) {
        // 1. 计算每个请求之间需要间隔的毫秒数
        // 例如：QPS=10，则 intervalMs = 100ms；QPS=100，则 intervalMs = 10ms
        long intervalMs = Math.max(1L, Math.round(1000D / qps));

        // 2. 计算 Redis Key 的过期时间 (TTL)
        // 这是一个保护机制，确保 Key 不会永久存在。
        // 这里设置为能处理 4 倍 permits 所需时间的长度，且最少为 5 秒。
        // 这样即使服务宕机，脏数据也会自动清理，防止内存泄漏。
        long ttlMs = Math.max(intervalMs * Math.max(permits, 1) * 4, 5000L);

        // 3. 执行 Lua 脚本
        Long waitMs = stringRedisTemplate.execute(
                leakyBucketRedisScript,
                List.of(RedisConstant.buildFlowControlLeakyKey(rateKey)),
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(intervalMs),
                String.valueOf(Math.max(permits, 1)),
                String.valueOf(ttlMs)
        );

        // 4. 处理返回结果，确保安全
        long safeWait = Math.max(waitMs, 0L);

        // 5. 如果需要等待，则阻塞当前线程
        // 这是一种客户端限流，通过延迟执行来平滑流量
        if (safeWait > 0) {
            sleep(safeWait);
        }
        return safeWait;
    }

    /**
     * 令牌桶：允许一定突发，更适合邮件、飞书这类通知型渠道。
     */
    private long applySecondWindow(String rateKey, double qps, int permits) {
        long totalWait = 0L;
        int maxPermitsPerSecond = Math.max(1, (int) Math.floor(qps));
        while (true) {
            long now = System.currentTimeMillis();
            String secondKey = RedisConstant.buildFlowControlSecondKey(rateKey, now / 1000);
            Long current = stringRedisTemplate.opsForValue().increment(secondKey, permits);
            if (current != null && current == permits) {
                stringRedisTemplate.expire(secondKey, 2, TimeUnit.SECONDS);
            }
            if (current != null && current <= maxPermitsPerSecond) {
                return totalWait;
            }
            long waitMs = 1000 - (now % 1000);
            sleep(waitMs);
            totalWait += waitMs;
        }
    }

    /**
     * 分钟 / 每日配额控制。
     */
    private void applyQuota(String rateKey, String type, Integer limit, int permits) {
        if (limit == null || limit <= 0) {
            return;
        }
        String quotaKey;
        long ttl;
        long now = System.currentTimeMillis();
        if ("minute".equals(type)) {
            quotaKey = RedisConstant.buildFlowControlMinuteKey(rateKey, now / 60000);
            ttl = 120L;
        } else {
            long epochDay = LocalDate.now().toEpochDay();
            quotaKey = RedisConstant.buildFlowControlDayKey(rateKey, epochDay);
            ttl = TimeUnit.DAYS.toSeconds(2);
        }
        Long current = stringRedisTemplate.opsForValue().increment(quotaKey, permits);
        if (current != null && current == permits) {
            stringRedisTemplate.expire(quotaKey, ttl, TimeUnit.SECONDS);
        }
        if (current != null && current > limit) {
            throw new FlowControlException(type + " quota exceeded, key=" + rateKey + ", limit=" + limit);
        }
    }

    /**
     * 生成当前消息的限流主键。
     */
    private String buildRateKey(TaskInfo taskInfo, FlowControlRule rule) {
        FlowControlScope scope = rule.getScope() == null ? FlowControlScope.CHANNEL_ACCOUNT : rule.getScope();
        StringBuilder builder = new StringBuilder("channel:").append(taskInfo.getSendChannel());
        if (scope == FlowControlScope.CHANNEL) {
            return builder.toString();
        }
        builder.append(":account:").append(taskInfo.getSendAccount() == null ? 0 : taskInfo.getSendAccount());
        if (scope == FlowControlScope.CHANNEL_ACCOUNT) {
            return builder.toString();
        }
        if (scope == FlowControlScope.CHANNEL_ACCOUNT_TENANT) {
            builder.append(":tenant:").append(normalize(taskInfo.getBusinessOwner()));
            return builder.toString();
        }
        String endpoint = resolveEndpointFingerprint(taskInfo);
        if (endpoint != null && !endpoint.isBlank()) {
            builder.append(":endpoint:").append(endpoint);
        }
        return builder.toString();
    }

    /**
     * 对 webhook 之类的真实 endpoint 做摘要，用于最细粒度限流。
     */
    private String resolveEndpointFingerprint(TaskInfo taskInfo) {
        if (taskInfo == null || taskInfo.getSendAccount() == null) {
            return null;
        }
        ChannelAccount channelAccount = accountUtils.getChannelAccountById(taskInfo.getSendAccount());
        if (channelAccount == null || channelAccount.getAccountConfig() == null) {
            return null;
        }
        String endpoint = null;
        Integer channel = taskInfo.getSendChannel();
        try {
            if (Objects.equals(channel, 25)) {
                endpoint = JSON.parseObject(channelAccount.getAccountConfig(), DingDingRobotAccount.class).getWebhook();
            } else if (Objects.equals(channel, 24)) {
                endpoint = JSON.parseObject(channelAccount.getAccountConfig(), FeiShuRobotAccount.class).getWebhook();
            } else if (Objects.equals(channel, 23)) {
                endpoint = JSON.parseObject(channelAccount.getAccountConfig(), EnterpriseWeChatRobotAccount.class).getWebhook();
            }
        } catch (Exception ex) {
            log.warn("parse endpoint fingerprint fail, account:{}, e:{}", taskInfo.getSendAccount(), ex.getMessage());
        }
        if (endpoint == null || endpoint.isBlank()) {
            return null;
        }
        return DigestUtils.md5DigestAsHex(endpoint.getBytes(StandardCharsets.UTF_8));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "default";
        }
        return value.trim().toLowerCase(Locale.ROOT).replace(':', '_');
    }

    private void sleep(long waitMs) {
        if (waitMs <= 0) {
            return;
        }
        try {
            Thread.sleep(waitMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FlowControlException("flow control interrupted");
        }
    }
}
