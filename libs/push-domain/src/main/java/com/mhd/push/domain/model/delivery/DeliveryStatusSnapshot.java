package com.mhd.push.domain.model.delivery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息投递状态快照。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryStatusSnapshot {
    /**
     * 0-未投递，1-发送中，2-已发送，3-发送失败。
     */
    private Integer status;

    /**
     * 发送失败原因。
     */
    private String errorMessage;

    /**
     * 更新时间（yyyy-MM-dd HH:mm:ss）。
     */
    private String updateTime;

    /**
     * 快照写入时间戳。
     */
    private Long timestamp;
}
