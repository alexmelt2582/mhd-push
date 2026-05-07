package com.mhd.push.publicapi.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 投递状态查询响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryStatusResponse {
    /**
     * 响应码。
     */
    private String code;

    /**
     * 响应说明。
     */
    private String msg;

    /**
     * 响应数据。
     */
    private DeliveryStatusData data;
}
