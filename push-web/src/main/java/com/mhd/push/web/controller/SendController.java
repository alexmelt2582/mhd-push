package com.mhd.push.web.controller;

import com.mhd.push.web.api.domain.BatchSendRequest;
import com.mhd.push.web.api.domain.SendRequest;
import com.mhd.push.web.api.domain.SendResponse;
import com.mhd.push.web.api.service.SendService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zhao-hao-dong
 **/
@RestController
public class SendController {
    @Resource
    private SendService sendService;

    ///**
    // * 发送消息接口
    // * 请注意body传参的时候Content-Type一定要设置为application/json，否则服务端没法正确识别body中的json格式。
    // */
    //@PostMapping("/send")
    //public BaseResponse<String> send(@RequestBody SendRequest sendRequest) {
    //    return sendService.send(sendRequest);
    //}
    //
    ///**
    // * 多渠道发送消息接口
    // * 参数与发送消息接口(/send)一样，差异在channel和option参数支持用逗号隔开来支持多个发送渠道。
    // * 请注意body传参的时候Content-Type一定要设置为application/json，否则服务端没法正确识别body中的json格式。
    // */
    //@PostMapping("/batchSend")
    //public BaseResponse<List<SendResponse>> batchSend(@RequestBody BatchSendRequest batchSendRequest) {
    //    return sendService.batchSend(batchSendRequest);
    //}

    /**
     * 单个文案下发相同的人
     */
    @PostMapping("/send")
    public SendResponse send(@RequestBody SendRequest sendRequest) {
        return sendService.send(sendRequest);
    }

    /**
     * 不同文案下发到不同的人
     */
    @PostMapping("/batchSend")
    public SendResponse batchSend(@RequestBody BatchSendRequest batchSendRequest) {
        return sendService.batchSend(batchSendRequest);
    }

    ///**
    // * 优先根据messageId撤回消息，如果messageId不存在则根据模板id撤回
    // */
    //@PostMapping("/recall")
    //public SendResponse recall(@RequestBody SendRequest sendRequest) {
    //    return recallService.recall(sendRequest);
    //}

}
