package com.mhd.push.web.api.service;

import com.mhd.push.web.api.domain.SendResponse;
import com.mhd.push.web.api.domain.SendRequest;
import com.mhd.push.web.api.domain.BatchSendRequest;

/**
 * 发送接口
 *
 * @author zhao-hao-dong

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
     * 单文案发送接口
     *
     * @param sendRequest eg:    {"code":"send","messageParam":{"bizId":null,"extra":null,"receiver":"123@qq.com","variables":null},"messageTemplateId":17,"recallMessageId":null}
     * @return SendResponse eg:    {"code":"0","data":[{"bizId":"ecZim2-FzdejNSY-sqgCM","businessId":2000001720230815,"messageId":"ecZim2-FzdejNSY-sqgCM"}],"msg":"操作成功"}
     */
    SendResponse send(SendRequest sendRequest);


    /**
     * 多文案发送接口
     *
     * @param batchSendRequest
     * @return
     */
    SendResponse batchSend(BatchSendRequest batchSendRequest);

}
