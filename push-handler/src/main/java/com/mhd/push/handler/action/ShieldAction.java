package com.mhd.push.handler.action;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.mhd.push.common.constant.RedisConstant;
import com.mhd.push.common.domain.MsgPushLogRequest;
import com.mhd.push.common.domain.TaskInfo;
import com.mhd.push.common.enums.MsgPushState;
import com.mhd.push.common.enums.MsgPushTypeEnum;
import com.mhd.push.common.enums.ShieldType;
import com.mhd.push.common.pipeline.BusinessProcess;
import com.mhd.push.common.pipeline.ProcessContext;
import com.mhd.push.support.utils.LogUtils;
import com.mhd.push.support.utils.RedisUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 屏蔽消息
 * 1. 当接收到该消息是夜间，直接屏蔽（不发送）
 * 2. 当接收到该消息是夜间，次日9点发送
 * example:当消息下发至austin平台时，已经是凌晨1点，业务希望此类消息在次日的早上9点推送
 * (配合 分布式任务定时任务框架搞掂)
 *
 * @author zhao-hao-dong
 */
@Service
public class ShieldAction implements BusinessProcess<TaskInfo> {
    private static final long SECONDS_OF_A_DAY = 86400L;
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private LogUtils logUtils;
    /**
     * 默认早上8点之前是凌晨
     */
    private static final int NIGHT = 8;

    @Override
    public void process(ProcessContext<TaskInfo> context) {
        TaskInfo taskInfo = context.getProcessModel();
        // 如果夜间不屏蔽，直接返回
        if (ShieldType.NIGHT_NO_SHIELD.getCode().equals(taskInfo.getShieldType())) {
            return;
        }
        if (LocalDateTime.now().getHour() < NIGHT) {
            // 夜间屏蔽
            if (ShieldType.NIGHT_SHIELD.getCode().equals(taskInfo.getShieldType())) {
                MsgPushLogRequest msgPushLogRequest = MsgPushLogRequest.builder()
                        .bizType(MsgPushTypeEnum.SEND.getCode())
                        .messageId(taskInfo.getMessageId())
                        .messageTemplateId(taskInfo.getMessageTemplateId())
                        .receiver(taskInfo.getReceiver())
                        .state(MsgPushState.NIGHT_SHIELD.getCode())
                        .stateDescription(MsgPushState.NIGHT_SHIELD.getDescription())
                        .timestamp(System.currentTimeMillis())
                        .build();
                logUtils.print(msgPushLogRequest);
            }
            // 夜间屏蔽（次日九点发送）
            if (ShieldType.NIGHT_SHIELD_BUT_NEXT_DAY_SEND.getCode().equals(taskInfo.getShieldType())) {
                redisUtils.lPush(RedisConstant.NIGHT_SHIELD_BUT_NEXT_DAY_SEND_KEY, JSON.toJSONString(taskInfo,
                                JSONWriter.Feature.WriteClassName),
                        SECONDS_OF_A_DAY);
                MsgPushLogRequest msgPushLogRequest = MsgPushLogRequest.builder()
                        .bizType(MsgPushTypeEnum.SEND.getCode())
                        .messageId(taskInfo.getMessageId())
                        .messageTemplateId(taskInfo.getMessageTemplateId())
                        .receiver(taskInfo.getReceiver())
                        .state(MsgPushState.NIGHT_SHIELD_NEXT_SEND.getCode())
                        .stateDescription(MsgPushState.NIGHT_SHIELD_NEXT_SEND.getDescription())
                        .timestamp(System.currentTimeMillis())
                        .build();
                logUtils.print(msgPushLogRequest);
            }
            context.setNeedBreak(true);
        }
    }
}
