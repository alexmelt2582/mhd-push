package com.mhd.push.publicapi.domain;

import com.mhd.push.publicapi.exception.ClientErrorCodeEnum;

/**
 * 发送结果VO
 *
 * @author zhao-hao-dong
 */
public record SendResultVO(
        String shortCode, // 流水号，用于查询最终发送结果
        String code, // 响应码
        String message, // 请求结果
        String channel // 发送渠道
) {
    public static SendResultVO build(String shortCode, ClientErrorCodeEnum clientErrorCodeEnum) {
        return new SendResultVO(shortCode, clientErrorCodeEnum.getCode(), clientErrorCodeEnum.getMessage(), null);
    }
}
