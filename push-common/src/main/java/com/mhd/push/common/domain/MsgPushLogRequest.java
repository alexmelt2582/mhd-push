package com.mhd.push.common.domain;

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
public class MsgPushLogRequest {
    /**
     * 业务类型
     *
     * @see com.mhd.push.common.enums.MsgPushTypeEnum
     */
    private String bizType;
    /**
     * 消息唯一Id(数据追踪使用)
     */
    private String messageId;
    /**
     * 消息模板Id
     */
    private Long messageTemplateId;
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
}
