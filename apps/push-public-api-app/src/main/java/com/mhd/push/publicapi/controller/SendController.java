package com.mhd.push.publicapi.controller;

import com.mhd.push.common.respnsedata.BaseResponse;
import com.mhd.push.common.respnsedata.BaseResultUtils;
import com.mhd.push.publicapi.domain.*;
import com.mhd.push.publicapi.service.SendService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公共发送接口控制器。
 * <p>
 * 对外提供发送、批量发送和发送状态查询入口。
 * </p>
 */
@RestController
@Validated
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
     * 单个文案下发相同的人。
     *
     * @param sendRequest 发送请求
     * @return 发送结果
     */
    @PostMapping("/send")
    public BaseResponse<List<SendResultVO>> send(@Valid @RequestBody SendRequest sendRequest) {
        List<SendResultVO> sendResultVOS = sendService.send(sendRequest);
        return BaseResultUtils.successOfData(sendResultVOS);
    }

    /**
     * 不同文案下发到不同的人。
     *
     * @param batchSendRequest 批量发送请求
     * @return 发送结果
     */
    @PostMapping("/batchSend")
    public BaseResponse<List<SendResultVO>> batchSend(@Valid @RequestBody BatchSendRequest batchSendRequest) {
        List<SendResultVO> sendResultVOS = sendService.batchSend(batchSendRequest);
        return BaseResultUtils.successOfData(sendResultVOS);
    }

    /**
     * 根据流水号 查询消息投递状态。
     *
     * @param shortCode 流水号
     * @return 状态查询结果
     */
    @GetMapping("/send/status")
    public DeliveryStatusResponse querySendStatus(@NotBlank(message = "流水号不能为空") String shortCode) {
        return sendService.queryStatusByTraceId(shortCode);
    }

    ///**
    // * 优先根据messageId撤回消息，如果messageId不存在则根据模板id撤回
    // */
    //@PostMapping("/recall")
    //public SendResponse recall(@RequestBody SendRequest sendRequest) {
    //    return recallService.recall(sendRequest);
    //}

}
