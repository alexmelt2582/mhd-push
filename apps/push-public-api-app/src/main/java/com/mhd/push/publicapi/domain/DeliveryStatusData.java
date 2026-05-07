package com.mhd.push.publicapi.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 投递状态查询数据。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryStatusData {
    /**
     * 0-未投递，1-发送中，2-已发送，3-发送失败。
     */
    private Integer status;

    /**
     * 发送失败原因。
     */
    private String errorMessage;

    /**
     * 更新时间。
     */
    private String updateTime;
}
