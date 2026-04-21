package com.mhd.push.handler.handler;

import cn.hutool.core.exceptions.ExceptionUtil;
import com.mhd.push.common.domain.MsgPushLogRequest;
import com.mhd.push.common.domain.TaskInfo;
import com.mhd.push.common.enums.MsgPushState;
import com.mhd.push.common.enums.MsgPushTypeEnum;
import com.mhd.push.handler.flowcontrol.FlowControlFactory;
import com.mhd.push.handler.flowcontrol.FlowControlParam;
import com.mhd.push.support.utils.LogUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 渠道处理器基类 (模板方法模式)
 * <p>
 * 职责：
 * 1. 定义消息发送的顶层流程（模板方法）。
 * 2. 处理通用的切面逻辑：限流、重试、日志记录、幂等守卫。
 * 3. 子类只需关注核心的 "handler" 逻辑实现。
 *
 * @author zhao-hao-dong
 */
@Slf4j
public abstract class BaseHandler implements Handler {
    /**
     * 标识渠道的Code
     * 子类初始化的时候指定
     */
    protected Integer channelCode;
    /**
     * 限流相关的参数
     * 子类初始化的时候指定
     */
    protected FlowControlParam flowControlParam;

    @Resource
    private HandlerHolder handlerHolder;
    @Resource
    private LogUtils logUtils;
    @Resource
    private FlowControlFactory flowControlFactory;
    @Resource
    private SendExecutionGuardService sendExecutionGuardService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Value("${mhd.handler.retry.max-attempts:3}")
    private int maxAttempts;
    @Value("${mhd.handler.retry.backoff-ms:200}")
    private long retryBackoffMs;

    /**
     * 初始化渠道与Handler的映射关系
     */
    @PostConstruct
    private void init() {
        handlerHolder.putHandler(channelCode, this);
    }

    /**
     * 核心执行流程 (模板方法)
     * 定义了不可变的业务骨架：幂等检查 -> 限流 -> 发送 -> 状态记录 -> 日志
     */
    @Override
    public void doHandler(TaskInfo taskInfo) {
        // 1. 幂等与执行守卫 (防止重复发送)
        SendGuardDecision decision = sendExecutionGuardService.tryStart(taskInfo, channelCode);
        if (decision == SendGuardDecision.ALREADY_SUCCESS) {
            logSuccess(taskInfo);
            return;
        }
        if (decision == SendGuardDecision.PENDING_CONFIRM) {
            logPendingConfirm(taskInfo);
            return;
        }

        // 2. 流量控制 (限流) 只有子类指定了限流参数，才需要限流
        boolean success = false;
        try {
            if (Objects.nonNull(flowControlParam)) {
                flowControlFactory.flowControl(taskInfo, flowControlParam);
            }
            success = retrySend(taskInfo);
        } catch (Exception ex) {
            success = false;
        }

        if (success) {
            sendExecutionGuardService.markSuccess(taskInfo, channelCode);
            logSuccess(taskInfo);
            return;
        }
        sendExecutionGuardService.markFail(taskInfo, channelCode);
        logFail(taskInfo);
    }

    private boolean retrySend(TaskInfo taskInfo) {
        int attempts = Math.max(1, maxAttempts);
        for (int i = 1; i <= attempts; i++) {
            try {
                if (handler(taskInfo)) {
                    return true;
                }
            } catch (Exception ex) {
                if (i >= attempts) {
                    return false;
                }
            }
            if (i < attempts) {
                try {
                    log.warn("BaseHandler#retrySend taskInfo retry {}/{} fail, messageId:{}, sleep:{}ms", i, attempts,
                            taskInfo.getMessageId(), retryBackoffMs);
                    Thread.sleep(retryBackoffMs);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    private void logSuccess(TaskInfo taskInfo) {
        logUtils.print(MsgPushLogRequest.builder()
                .bizType(MsgPushTypeEnum.SEND.getCode())
                .messageId(taskInfo.getMessageId())
                .messageTemplateId(taskInfo.getMessageTemplateId())
                .receiver(taskInfo.getReceiver())
                .state(MsgPushState.SEND_SUCCESS.getCode())
                .stateDescription(MsgPushState.SEND_SUCCESS.getDescription())
                .timestamp(System.currentTimeMillis())
                .build());
    }

    private void logFail(TaskInfo taskInfo) {
        logUtils.print(MsgPushLogRequest.builder()
                .bizType(MsgPushTypeEnum.SEND.getCode())
                .messageId(taskInfo.getMessageId())
                .messageTemplateId(taskInfo.getMessageTemplateId())
                .receiver(taskInfo.getReceiver())
                .state(MsgPushState.SEND_FAIL.getCode())
                .stateDescription(MsgPushState.SEND_FAIL.getDescription())
                .timestamp(System.currentTimeMillis())
                .build());
    }

    private void logPendingConfirm(TaskInfo taskInfo) {
        logUtils.print(MsgPushLogRequest.builder()
                .bizType(MsgPushTypeEnum.SEND.getCode())
                .messageId(taskInfo.getMessageId())
                .messageTemplateId(taskInfo.getMessageTemplateId())
                .receiver(taskInfo.getReceiver())
                .state(MsgPushState.SEND_PENDING_CONFIRM.getCode())
                .stateDescription(MsgPushState.SEND_PENDING_CONFIRM.getDescription())
                .timestamp(System.currentTimeMillis())
                .build());
    }

    protected void recordExternalRateLimitBackoff(TaskInfo taskInfo, long retryAfterMs) {
        flowControlFactory.recordBackoff(taskInfo, flowControlParam, retryAfterMs);
    }

    /**
     * 统一处理的handler接口
     */
    public abstract boolean handler(TaskInfo taskInfo);

    /**
     * 将撤回的消息存储到redis
     *
     * @param prefix            redis前缀
     * @param messageTemplateId 消息模板id
     * @param taskId            消息下发taskId
     * @param expireTime        存储到redis的有效时间（跟对应渠道可撤回多久的消息有关系)
     */
    protected void saveRecallInfo(String prefix, Long messageTemplateId, String taskId, Long expireTime) {
        redisTemplate.opsForList().leftPush(prefix + messageTemplateId, taskId);
        redisTemplate.opsForValue().set(prefix + taskId, taskId);
        redisTemplate.expire(prefix + messageTemplateId, expireTime, TimeUnit.SECONDS);
        redisTemplate.expire(prefix + taskId, expireTime, TimeUnit.SECONDS);
    }
}
