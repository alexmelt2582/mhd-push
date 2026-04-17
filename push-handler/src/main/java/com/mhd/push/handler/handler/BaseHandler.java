package com.mhd.push.handler.handler;

import com.mhd.push.common.domain.AnchorInfo;
import com.mhd.push.common.domain.MsgPushLogRequest;
import com.mhd.push.common.domain.TaskInfo;
import com.mhd.push.common.enums.MsgPushState;
import com.mhd.push.common.enums.MsgPushTypeEnum;
import com.mhd.push.handler.flowcontrol.FlowControlFactory;
import com.mhd.push.handler.flowcontrol.FlowControlParam;
import com.mhd.push.support.utils.LogUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 发送各个渠道的handler
 *
 * @author zhao-hao-dong
 */
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
    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 初始化渠道与Handler的映射关系
     */
    @PostConstruct
    private void init() {
        handlerHolder.putHandler(channelCode, this);
    }

    @Override
    public void doHandler(TaskInfo taskInfo) {
        // 只有子类指定了限流参数，才需要限流
        if (Objects.nonNull(flowControlParam)) {
            flowControlFactory.flowControl(taskInfo, flowControlParam);
        }
        if (handler(taskInfo)) {
            MsgPushLogRequest msgPushLogRequest = MsgPushLogRequest.builder()
                    .bizType(MsgPushTypeEnum.SEND.getCode())
                    .messageId(taskInfo.getMessageId())
                    .messageTemplateId(taskInfo.getMessageTemplateId())
                    .receiver(taskInfo.getReceiver())
                    .state(MsgPushState.SEND_SUCCESS.getCode())
                    .stateDescription(MsgPushState.SEND_SUCCESS.getDescription())
                    .timestamp(System.currentTimeMillis())
                    .build();
            logUtils.print(msgPushLogRequest);
            return;
        }
        MsgPushLogRequest msgPushLogRequest = MsgPushLogRequest.builder()
                .bizType(MsgPushTypeEnum.SEND.getCode())
                .messageId(taskInfo.getMessageId())
                .messageTemplateId(taskInfo.getMessageTemplateId())
                .receiver(taskInfo.getReceiver())
                .state(MsgPushState.SEND_FAIL.getCode())
                .stateDescription(MsgPushState.SEND_FAIL.getDescription())
                .timestamp(System.currentTimeMillis())
                .build();
        logUtils.print(msgPushLogRequest);
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
