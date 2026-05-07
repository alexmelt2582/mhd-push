//package com.mhd.push.publicapi.controller;
//
//import cn.hutool.core.text.CharSequenceUtil;
//import com.mhd.push.publicapi.domain.dto.PendingConfirmConfirmDTO;
//import com.mhd.push.publicapi.service.PendingConfirmService;
//import com.mhd.push.common.respnsedata.BaseResponse;
//import com.mhd.push.common.respnsedata.BaseResultUtils;
//import com.mhd.push.engine.handler.SendExecutionRecord;
//import jakarta.annotation.Resource;
//import jakarta.validation.Valid;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Collections;
//import java.util.List;
//
///**
// * 待确认消息运维接口。
// * <p>
// * 作用：把 SEND_PENDING_CONFIRM 从“只有日志状态”升级成“可查询、可人工确认”的闭环。
// */
//@RestController
//@RequestMapping("/delivery/pendingConfirm")
//public class PendingConfirmController extends BaseController {
//
//    @Resource
//    private PendingConfirmService pendingConfirmService;
//
//    /**
//     * 查询最近的待确认消息。
//     */
//    @GetMapping("/list")
//    public BaseResponse<List<SendExecutionRecord>> list(@RequestParam(defaultValue = "20") Integer limit) {
//        return BaseResultUtils.successOfData(pendingConfirmService.listPendingRecords(limit));
//    }
//
//    /**
//     * 按 traceId 查询待确认记录。
//     */
//    @GetMapping("/query")
//    public BaseResponse<List<SendExecutionRecord>> query(@RequestParam String traceId) {
//        if (CharSequenceUtil.isBlank(traceId)) {
//            return BaseResultUtils.successOfData(Collections.emptyList());
//        }
//        return BaseResultUtils.successOfData(pendingConfirmService.queryByTraceId(traceId));
//    }
//
//    /**
//     * 人工确认成功：表示第三方其实已经收到了，只是本地未完成确认。
//     */
//    @PostMapping("/confirmSuccess")
//    public BaseResponse<Void> confirmSuccess(@Valid @RequestBody PendingConfirmConfirmDTO request) {
//        return toAjax(pendingConfirmService.confirmSuccess(request.getExecutionKey()));
//    }
//
//    /**
//     * 人工确认失败：允许该条记录后续重新发送。
//     */
//    @PostMapping("/confirmFail")
//    public BaseResponse<Void> confirmFail(@Valid @RequestBody PendingConfirmConfirmDTO request) {
//        return toAjax(pendingConfirmService.confirmFail(request.getExecutionKey()));
//    }
//}