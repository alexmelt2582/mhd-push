package com.mhd.push.common.log;

import cn.hutool.core.util.StrUtil;
import com.mhd.push.common.enums.MsgPushState;
import com.mhd.push.common.enums.SendTypeEnum;
import com.mhd.push.common.utils.TaskInfoUtils;
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
     * @see SendTypeEnum
     */
    private String sendType;
    /**
     * 消息模板ID
     */
    private Long templateId;

    /**
     * 业务ID（模板类型+模板ID+日期）
     */
    private Long businessId;
    /**
     * 接收者
     */
    private Set<String> receiver;

    /**
     * 用户传入的结果回调地址（可选）。
     */
    private String callbackUrl;
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

    public static LogRecord build(SendTypeEnum sendTypeEnum, LogTaskInfo taskInfo, MsgPushState msgPushState) {
        return LogRecord.builder()
                .traceId(taskInfo.getTraceId())
                .sendType(sendTypeEnum.getCode())
                .templateId(taskInfo.getTemplateId())
                .businessId(TaskInfoUtils.generateBusinessId(taskInfo.getTemplateId(), taskInfo.getTemplateType()))
                .receiver(taskInfo.getReceiver())
                .callbackUrl(taskInfo.getCallbackUrl())
                .state(msgPushState.getCode())
                .stateDescription(msgPushState.getDescription())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static LogRecord build(SendTypeEnum sendTypeEnum, LogTaskInfo taskInfo, MsgPushState msgPushState, String stateDescription) {
        return LogRecord.builder()
                .traceId(taskInfo.getTraceId())
                .sendType(sendTypeEnum.getCode())
                .templateId(taskInfo.getTemplateId())
                .businessId(TaskInfoUtils.generateBusinessId(taskInfo.getTemplateId(), taskInfo.getTemplateType()))
                .receiver(taskInfo.getReceiver())
                .callbackUrl(taskInfo.getCallbackUrl())
                .state(msgPushState.getCode())
                .stateDescription(StrUtil.isNotBlank(stateDescription) ? stateDescription : msgPushState.getDescription())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static LogRecord build(SendTypeEnum sendTypeEnum, LogTaskInfo taskInfo, int state, String stateDescription) {
        return LogRecord.builder()
                .traceId(taskInfo.getTraceId())
                .sendType(sendTypeEnum.getCode())
                .templateId(taskInfo.getTemplateId())
                .businessId(TaskInfoUtils.generateBusinessId(taskInfo.getTemplateId(), taskInfo.getTemplateType()))
                .receiver(taskInfo.getReceiver())
                .callbackUrl(taskInfo.getCallbackUrl())
                .state(state)
                .stateDescription(stateDescription)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
