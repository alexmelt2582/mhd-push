package com.mhd.push.publicapi.service;


import com.mhd.push.publicapi.domain.*;

import java.util.List;

/**
 * 发送服务接口。
 */
public interface SendService {

    ///**
    // * 单文案发送接口
    // */
    //BaseResponse<String> send(SendRequest sendRequest);
    //
    ///**
    // * 多文案发送接口
    // */
    //BaseResponse<List<SendResponse>> batchSend(BatchSendRequest batchSendRequest);

    /**
     * 单文案发送接口。
     *
     * @param sendRequest 单次发送请求
     * @return 发送结果
     */
    List<SendResultVO> send(SendRequest sendRequest);

    /**
     * 多文案发送接口。
     *
     * @param batchSendRequest 批量发送请求
     * @return 发送结果
     */
    List<SendResultVO> batchSend(BatchSendRequest batchSendRequest);

    /**
     * 根据链路 ID 查询消息投递状态。
     *
     * @param traceId 链路 ID
     * @return 投递状态
     */
    DeliveryStatusResponse queryStatusByTraceId(String traceId);

}
