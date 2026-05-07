package com.mhd.push.infra.utils;

import cn.monitor4all.logRecord.bean.LogDTO;
import cn.monitor4all.logRecord.service.CustomLogListener;
import com.alibaba.fastjson2.JSON;
import com.google.common.base.Throwables;
import com.mhd.push.common.constant.GlobalConstant;
import com.mhd.push.common.constant.ThreadPoolConstant;
import com.mhd.push.common.enums.MsgPushState;
import com.mhd.push.common.log.LogParam;
import com.mhd.push.common.log.LogRecord;
import com.mhd.push.common.utils.TaskInfoUtils;
import com.mhd.push.domain.model.delivery.DeliveryStatusSnapshot;
import com.mhd.push.domain.model.task.TaskInfo;
import com.mhd.push.domain.model.task.SimpleAnchorInfo;
import com.mhd.push.infra.mq.SendMqService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 所有的日志都存在
 *
 * @author zhao-hao-dong
 */
@Slf4j
@Component
public class LogUtils extends CustomLogListener {
    private static final String REDIS_KEY_SEPARATOR = ":";

    @Autowired
    private SendMqService sendMqService;
    @Autowired
    private RedisUtils redisUtils;

    @Value("${mhd.mq.topic.log}")
    private String logTopic;
    @Value("${mhd.trace.message-id.ttl-seconds:604800}")
    private long messageTraceTtlSeconds;
    @Value("${mhd.callback.enabled:true}")
    private boolean callbackEnabled;
    @Value("${mhd.callback.max-attempts:3}")
    private int callbackMaxAttempts;
    @Value("${mhd.callback.retry-backoff-ms:300}")
    private long callbackRetryBackoffMs;
    @Value("${mhd.callback.connect-timeout-ms:1000}")
    private long callbackConnectTimeoutMs;
    @Value("${mhd.callback.read-timeout-ms:2000}")
    private long callbackReadTimeoutMs;
    @Value("${mhd.callback.executor-threads:2}")
    private int callbackExecutorThreads;

    private ExecutorService callbackExecutor;
    private HttpClient callbackHttpClient;

    /**
     * 初始化回调执行器与HTTP客户端。
     */
    @PostConstruct
    public void initCallbackInfrastructure() {
        // 使用独立线程池执行回调，避免阻塞核心发送链路线程。
        int threadSize = Math.max(callbackExecutorThreads, ThreadPoolConstant.COMMON_CORE_POOL_SIZE);
        this.callbackExecutor = Executors.newFixedThreadPool(threadSize);
        this.callbackHttpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(callbackConnectTimeoutMs, 100L)))
                .build();
    }

    /**
     * 关闭回调执行器，释放资源。
     */
    @PreDestroy
    public void shutdownCallbackInfrastructure() {
        if (callbackExecutor != null) {
            callbackExecutor.shutdown();
        }
    }

    /**
     * 方法切面的日志 @OperationLog 所产生
     */
    @Override
    public void createLog(LogDTO logDTO) {
        log.info("LogUtils#createLog logDTO: {}", JSON.toJSONString(logDTO));
    }

    /**
     * 记录当前对象信息
     */
    public void print(LogParam logParam) {
        logParam.setTimestamp(System.currentTimeMillis());
        log.info("LogUtils#print logParam: {}", JSON.toJSONString(logParam));
    }

    /**
     * 记录打点信息
     */
    public void print(LogRecord logRecord) {
        // 1. 先写埋点明细，保证链路可追踪。
        writeTraceAnchor(logRecord);

        // 2. 再写状态快照，提升高频状态查询的读性能。
        writeStatusSnapshot(logRecord);

        // 3. 最后尝试终态回调（异步 + 重试），不阻塞主流程。
        sendCallbackAsync(logRecord);

        // 4. 保持原有日志Topic投递，兼容离线分析与审计链路。
        String message = JSON.toJSONString(logRecord);
        log.info("LogUtils#print logRecord: {}", message);
        try {
            sendMqService.send(logTopic, message);
        } catch (Exception e) {
            log.error("LogUtils#print send mq fail! e:{},params:{}", Throwables.getStackTraceAsString(e)
                    , JSON.toJSONString(message));
        }
    }

    /**
     * 记录打点信息
     */
    public void printWarn(LogRecord logRecord) {
        // 1. 先写埋点明细，保证链路可追踪。
        writeTraceAnchor(logRecord);

        // 2. 再写状态快照，提升高频状态查询的读性能。
        writeStatusSnapshot(logRecord);

        // 3. 最后尝试终态回调（异步 + 重试），不阻塞主流程。
        sendCallbackAsync(logRecord);

        // 4. 保持原有日志Topic投递，兼容离线分析与审计链路。
        String message = JSON.toJSONString(logRecord);
        log.warn("LogUtils#print logRecord: {}", message);
        try {
            sendMqService.send(logTopic, message);
        } catch (Exception e) {
            log.error("LogUtils#print send mq fail! e:{},params:{}", Throwables.getStackTraceAsString(e)
                    , JSON.toJSONString(message));
        }
    }

    /**
     * 记录打点信息
     */
    public void printError(LogRecord logRecord) {
        // 1. 先写埋点明细，保证链路可追踪。
        writeTraceAnchor(logRecord);

        // 2. 再写状态快照，提升高频状态查询的读性能。
        writeStatusSnapshot(logRecord);

        // 3. 最后尝试终态回调（异步 + 重试），不阻塞主流程。
        sendCallbackAsync(logRecord);

        // 4. 保持原有日志Topic投递，兼容离线分析与审计链路。
        String message = JSON.toJSONString(logRecord);
        log.error("LogUtils#print logRecord: {}", message);
        try {
            sendMqService.send(logTopic, message);
        } catch (Exception e) {
            log.error("LogUtils#print send mq fail! e:{},params:{}", Throwables.getStackTraceAsString(e)
                    , JSON.toJSONString(message));
        }
    }

    /**
     * 将链路埋点写入 Redis 列表。
     *
     * @param logRecord 当前埋点记录
     */
    private void writeTraceAnchor(LogRecord logRecord) {
        if (logRecord == null || logRecord.getTraceId() == null || logRecord.getTraceId().isBlank()) {
            return;
        }
        // 按统一结构持久化埋点，兼容既有 /trace/message 查询逻辑。
        SimpleAnchorInfo anchorInfo = SimpleAnchorInfo.builder()
                .state(logRecord.getState())
                .businessId(logRecord.getBusinessId())
                .stage("DELIVERY")
                .result(resolveTraceResult(logRecord.getState()))
                .logLevel(resolveTraceLogLevel(logRecord.getState()))
                .description(logRecord.getStateDescription())
                .timestamp(logRecord.getTimestamp())
                .build();
        writeTraceAnchor(logRecord.getTraceId(), anchorInfo);
    }

    /**
     * 写入内部节点埋点，但不更新对外状态快照。
     *
     * @param taskInfo 任务信息
     * @param stage 节点阶段
     * @param result 节点结果
     * @param description 节点说明
     * @param logLevel 日志级别
     */
    public void printTraceNode(TaskInfo taskInfo, String stage, String result, String description, String logLevel) {
        // 无 traceId 时无法归档到链路，直接跳过。
        if (taskInfo == null || taskInfo.getTraceId() == null || taskInfo.getTraceId().isBlank()) {
            return;
        }
        // 使用任务维度生成 businessId，保证 admin 页面仍可按模板业务分组展示。
        Long businessId = TaskInfoUtils.generateBusinessId(taskInfo.getTemplateId(), taskInfo.getTemplateType());
        SimpleAnchorInfo anchorInfo = SimpleAnchorInfo.builder()
                .state(0)
                .businessId(businessId)
                .stage(stage)
                .result(result)
                .logLevel(logLevel)
                .description(description)
                .timestamp(System.currentTimeMillis())
                .build();
        writeTraceAnchor(taskInfo.getTraceId(), anchorInfo);
    }

    /**
     * 将埋点对象写入 Redis 链路列表。
     *
     * @param traceId 链路ID
     * @param anchorInfo 埋点信息
     */
    private void writeTraceAnchor(String traceId, SimpleAnchorInfo anchorInfo) {
        // 基础字段不完整时不写入，避免污染链路数据。
        if (traceId == null || traceId.isBlank() || anchorInfo == null) {
            return;
        }
        // 使用统一 key 追加到 trace 列表，供 admin 页面按时间回放完整链路。
        redisUtils.lPush(buildTraceAnchorKey(traceId), JSON.toJSONString(anchorInfo), messageTraceTtlSeconds);
    }

    /**
     * 将链路终态/中间态写入 Redis 单键快照。
     *
     * @param logRecord 当前埋点记录
     */
    private void writeStatusSnapshot(LogRecord logRecord) {
        if (logRecord == null || logRecord.getTraceId() == null || logRecord.getTraceId().isBlank()) {
            return;
        }

        // 根据埋点状态码计算对外投递状态。
        int deliveryStatus = mapDeliveryStatus(logRecord.getState());
        String errorMessage = resolveExternalErrorMessage(logRecord, deliveryStatus);
        String updateTime = formatTimestamp(logRecord.getTimestamp());
        DeliveryStatusSnapshot snapshot = DeliveryStatusSnapshot.builder()
                .status(deliveryStatus)
                .errorMessage(errorMessage)
                .updateTime(updateTime)
                .timestamp(System.currentTimeMillis())
                .build();

        // 使用 setex 语义写入单键快照，查询端可 O(1) 读取。
        redisUtils.pipelineSetEx(Map.of(buildStatusSnapshotKey(logRecord.getTraceId()), JSON.toJSONString(snapshot)), messageTraceTtlSeconds);
    }

    /**
     * 触发终态回调。
     *
     * @param logRecord 当前埋点记录
     */
    private void sendCallbackAsync(LogRecord logRecord) {
        if (!callbackEnabled || !isCallbackReady(logRecord)) {
            return;
        }

        // 回调只针对终态（成功/失败），避免中间态造成回调风暴。
        int deliveryStatus = mapDeliveryStatus(logRecord.getState());
        if (!isTerminalStatus(deliveryStatus)) {
            return;
        }

        callbackExecutor.submit(() -> {
            int maxAttempts = Math.max(callbackMaxAttempts, 1);
            for (int i = 1; i <= maxAttempts; i++) {
                if (sendCallbackRequest(logRecord, deliveryStatus)) {
                    return;
                }
                if (i < maxAttempts) {
                    try {
                        Thread.sleep(Math.max(callbackRetryBackoffMs, 50L));
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        });
    }

    /**
     * 真正执行HTTP回调请求。
     *
     * @param logRecord 当前埋点记录
     * @param deliveryStatus 对外投递状态
     * @return 回调是否成功
     */
    private boolean sendCallbackRequest(LogRecord logRecord, int deliveryStatus) {
        try {
            String callbackBody = buildCallbackBody(logRecord, deliveryStatus);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(logRecord.getCallbackUrl()))
                    .timeout(Duration.ofMillis(Math.max(callbackReadTimeoutMs, 200L)))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(callbackBody))
                    .build();
            HttpResponse<String> response = callbackHttpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return true;
            }
            log.warn("callback fail, traceId:{}, callbackUrl:{}, httpCode:{}", logRecord.getTraceId(), logRecord.getCallbackUrl(), response.statusCode());
            return false;
        } catch (Exception ex) {
            log.warn("callback exception, traceId:{}, callbackUrl:{}, err:{}", logRecord.getTraceId(), logRecord.getCallbackUrl(), ex.getMessage());
            return false;
        }
    }

    /**
     * 组装回调请求体。
     *
     * @param logRecord 当前埋点记录
     * @param deliveryStatus 对外投递状态
     * @return JSON字符串
     */
    private String buildCallbackBody(LogRecord logRecord, int deliveryStatus) {
        String errorMessage = resolveExternalErrorMessage(logRecord, deliveryStatus);
        String updateTime = formatTimestamp(logRecord.getTimestamp());
        return JSON.toJSONString(Map.of(
                "code", "200",
                "msg", "操作成功",
                "data", Map.of(
                        "status", deliveryStatus,
                        "errorMessage", errorMessage,
                        "updateTime", updateTime,
                        "messageId", logRecord.getTraceId(),
                        "state", logRecord.getState(),
                        "stateDescription", logRecord.getStateDescription()
                )
        ));
    }

    /**
     * 判断回调基础条件是否满足。
     *
     * @param logRecord 当前埋点记录
     * @return true 表示可以尝试回调
     */
    private boolean isCallbackReady(LogRecord logRecord) {
        if (logRecord == null || logRecord.getTraceId() == null || logRecord.getTraceId().isBlank()) {
            return false;
        }
        if (logRecord.getCallbackUrl() == null || logRecord.getCallbackUrl().isBlank()) {
            return false;
        }
        return isHttpUrl(logRecord.getCallbackUrl());
    }

    /**
     * 判断地址是否为 http/https。
     *
     * @param callbackUrl 回调地址
     * @return true 表示协议合法
     */
    private boolean isHttpUrl(String callbackUrl) {
        try {
            URI uri = URI.create(callbackUrl);
            String scheme = uri.getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * 将埋点状态码映射为对外投递状态。
     *
     * @param stateCode 埋点状态码
     * @return 对外投递状态
     */
    private int mapDeliveryStatus(int stateCode) {
        // 优先使用统一状态枚举映射，避免失败态规则在多个模块散落。
        MsgPushState msgPushState = MsgPushState.findByCode(stateCode);
        if (msgPushState == null) {
            return MsgPushState.DELIVERY_SENDING;
        }
        return msgPushState.toExternalDeliveryStatus();
    }

    /**
     * 获取对外失败原因。
     *
     * @param logRecord 当前埋点记录
     * @param deliveryStatus 对外投递状态
     * @return 对外失败原因
     */
    private String resolveExternalErrorMessage(LogRecord logRecord, int deliveryStatus) {
        // 非失败态不返回错误信息，避免外部用户看到内部处理中间态描述。
        if (deliveryStatus != MsgPushState.DELIVERY_FAIL || logRecord == null) {
            return "";
        }
        // 已知内部状态优先走统一映射，保证对外口径一致。
        MsgPushState msgPushState = MsgPushState.findByCode(logRecord.getState());
        if (msgPushState != null) {
            return msgPushState.toExternalErrorMessage();
        }
        // 未知状态兜底返回通用失败提示，不把第三方错误细节直接暴露给外部。
        return MsgPushState.SEND_FAIL.getDescription();
    }

    /**
     * 根据内部状态计算 trace 展示结果。
     *
     * @param stateCode 内部状态码
     * @return trace 结果标识
     */
    private String resolveTraceResult(int stateCode) {
        // 成功态直接标记为 PASS。
        MsgPushState msgPushState = MsgPushState.findByCode(stateCode);
        if (msgPushState == null) {
            return "FAIL";
        }
        if (msgPushState.isExternalSuccessState()) {
            return "PASS";
        }
        if (msgPushState.isExternalFailureState()) {
            return "FAIL";
        }
        return "PENDING";
    }

    /**
     * 根据内部状态计算 trace 展示级别。
     *
     * @param stateCode 内部状态码
     * @return trace 日志级别
     */
    private String resolveTraceLogLevel(int stateCode) {
        // 成功态写 INFO，失败态写 ERROR，中间态写 WARN。
        MsgPushState msgPushState = MsgPushState.findByCode(stateCode);
        if (msgPushState == null) {
            return "ERROR";
        }
        if (msgPushState.isExternalSuccessState()) {
            return "INFO";
        }
        if (msgPushState.isExternalFailureState()) {
            return "ERROR";
        }
        return "WARN";
    }

    /**
     * 判断是否为终态。
     *
     * @param deliveryStatus 投递状态
     * @return true 表示终态
     */
    private boolean isTerminalStatus(int deliveryStatus) {
        return deliveryStatus == MsgPushState.DELIVERY_SUCCESS || deliveryStatus == MsgPushState.DELIVERY_FAIL;
    }

    /**
     * 格式化时间戳。
     *
     * @param timestamp 时间戳
     * @return 时间字符串
     */
    private String formatTimestamp(long timestamp) {
        return timestamp <= 0 ? "" : new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp));
    }

    /**
     * 构造埋点列表Key。
     *
     * @param traceId 链路ID
     * @return Redis Key
     */
    private String buildTraceAnchorKey(String traceId) {
        return GlobalConstant.CACHE_KEY_PREFIX
                + REDIS_KEY_SEPARATOR
                + GlobalConstant.MESSAGE_ID
                + REDIS_KEY_SEPARATOR
                + traceId;
    }

    /**
     * 构造状态快照Key。
     *
     * @param traceId 链路ID
     * @return Redis Key
     */
    private String buildStatusSnapshotKey(String traceId) {
        return GlobalConstant.CACHE_KEY_PREFIX
                + REDIS_KEY_SEPARATOR
                + GlobalConstant.MESSAGE_STATUS
                + REDIS_KEY_SEPARATOR
                + traceId;
    }
}
