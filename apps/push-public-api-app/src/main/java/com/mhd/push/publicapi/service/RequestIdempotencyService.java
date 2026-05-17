package com.mhd.push.publicapi.service;

import com.alibaba.fastjson2.JSON;
import com.mhd.push.common.constant.RedisConstant;
import com.mhd.push.publicapi.domain.SendResultVO;
import com.mhd.push.publicapi.exception.ClientBusinessException;
import com.mhd.push.publicapi.exception.ClientErrorCodeEnum;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 接口级幂等处理服务。
 * <p>
 * 负责为发送接口生成请求指纹、加锁防重，并缓存成功结果供重复请求复用。
 * </p>
 */
@Service
@Slf4j
public class RequestIdempotencyService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${mhd.idempotency.enabled:true}")
    private boolean enabled;
    @Value("${mhd.idempotency.lock-ttl-seconds:10}")
    private long lockTtlSeconds;
    @Value("${mhd.idempotency.result-ttl-seconds:300}")
    private long resultTtlSeconds;

    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "   return redis.call('del', KEYS[1]) " +
                    "else " +
                    "   return 0 " +
                    "end";

    private DefaultRedisScript<Long> unlockScript;

    @PostConstruct
    public void init() {
        unlockScript = new DefaultRedisScript<>();
        unlockScript.setScriptText(UNLOCK_SCRIPT);
        unlockScript.setResultType(Long.class);
    }

    /**
     * 计算请求指纹。
     *
     * @param apiName 接口名称
     * @param payload 请求体
     * @return 指纹值
     */
    public String buildRequestFingerprint(String apiName, Object payload) {
        // 1. 接口名与请求体一起参与计算，避免不同接口的相同负载互相冲突。
        String raw = apiName + ":" + JSON.toJSONString(payload);
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 在真正处理请求前进行幂等校验。（加锁）
     *
     * @param idempotencyKey 幂等键
     * @return 命中的历史结果，或进行中的提示，未命中则返回 null
     */
    public List<SendResultVO> preCheck(String idempotencyKey, String uuid) {
        if (!enabled) {
            return null;
        }
        // 1. 已有成功结果时直接返回缓存响应。
        String result = stringRedisTemplate.opsForValue().get(RedisConstant.buildIdempotencyResultKey(idempotencyKey));
        if (result != null) {
            return JSON.parseArray(result, SendResultVO.class);
        }

        // 2. 未命中结果时尝试获取短期锁，防止并发重复提交。
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(RedisConstant.buildIdempotencyLockKey(idempotencyKey), uuid, lockTtlSeconds, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            throw new ClientBusinessException(ClientErrorCodeEnum.CLIENT_SEND_IN_PROGRESS);
        }
        return null;
    }

    /**
     * 在请求成功后写入结果缓存并释放锁。
     *
     * @param idempotencyKey 幂等键
     * @param sendResultVOList 响应结果
     */
    public void onSuccess(String idempotencyKey, List<SendResultVO> sendResultVOList, String uuid) {
        if (!enabled) {
            return;
        }
        try {
            // 1. 存结果
            String resultKey = RedisConstant.buildIdempotencyResultKey(idempotencyKey);
            stringRedisTemplate.opsForValue().set(resultKey, JSON.toJSONString(sendResultVOList), resultTtlSeconds, TimeUnit.SECONDS);
        } finally {
            // 2. 无论如何都要尝试释放锁
            // 传入原始 key，让 onFail 内部处理前缀
            onFail(idempotencyKey, uuid);
        }
    }

    /**
     * 在请求失败后释放幂等锁。
     *
     * @param idempotencyKey 幂等键
     */
    public void onFail(String idempotencyKey, String uuid) {
        if (!enabled) {
            return;
        }
        try {
            // 只有当 Redis 中存储的值等于 uuid 时，才执行删除
            Long result = stringRedisTemplate.execute(
                    unlockScript,
                    Collections.singletonList(RedisConstant.buildIdempotencyLockKey(idempotencyKey)),
                    uuid
            );

            if (result == 0L) {
                // 这里通常意味着锁已经过期被系统删了，或者被别人删了（极低概率）
                // 记录一条警告日志即可
                log.warn("幂等锁释放失败（可能已过期或被误删），key: {}", idempotencyKey);
            }
        } catch (Exception e) {
            // 记录异常日志
            log.error("幂等锁释放异常，key: {}, error: {}", idempotencyKey, e.getMessage(), e);
        }
    }
}
