package com.mhd.push.common.domain;

import com.mhd.push.common.enums.MsgPushState;
import com.mhd.push.common.enums.SendTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * 日志请求实体
 *
 * @author zhao-hao-dong
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LogRecord {
    /**
     * 数据追踪唯一ID
     */
    private String traceId;
    /**
     * 业务类型
     *
     * @see com.mhd.push.common.enums.SendTypeEnum
     */
    private String sendType;
    /**
     * 消息模板ID
     */
    private Long templateId;
    /**
     * 接收者
     */
    private Set<String> receiver;
    /**
     * 具体点位
     */
    private int state;
    /**
     * 具体点位描述
     */
    private String stateDescription;
    /**
     * 生成时间
     */
    private long timestamp;

    public static LogRecord build(SendTypeEnum sendTypeEnum, TaskInfo taskInfo, MsgPushState msgPushState) {
        return LogRecord.builder()
                .traceId(taskInfo.getTraceId())
                .sendType(sendTypeEnum.getCode())
                .templateId(taskInfo.getTemplateId())
                .receiver(taskInfo.getReceiver())
                .state(msgPushState.getCode())
                .stateDescription(msgPushState.getDescription())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static LogRecord build(SendTypeEnum sendTypeEnum, TaskInfo taskInfo, int state, String stateDescription) {
        return LogRecord.builder()
                .traceId(taskInfo.getTraceId())
                .sendType(sendTypeEnum.getCode())
                .templateId(taskInfo.getTemplateId())
                .receiver(taskInfo.getReceiver())
                .state(state)
                .stateDescription(stateDescription)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
