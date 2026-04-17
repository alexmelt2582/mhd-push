package com.mhd.push.web.controller;

import com.mhd.push.common.domain.MqDlqRecord;
import com.mhd.push.common.enums.ErrorCodeEnum;
import com.mhd.push.common.respnsedata.BaseResponse;
import com.mhd.push.common.respnsedata.BaseResultUtils;
import com.mhd.push.web.service.DlqCompensationService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * DLQ消息回访与人工补偿接口
 */
@RestController
@RequestMapping("/mq/dlq")
public class DlqController {

    @Resource
    private DlqCompensationService dlqCompensationService;

    @GetMapping("/list")
    public BaseResponse<List<MqDlqRecord>> list(@RequestParam(defaultValue = "1") Integer pageNo,
                                                @RequestParam(defaultValue = "20") Integer pageSize) {
        return BaseResultUtils.successOfData(dlqCompensationService.list(pageNo, pageSize));
    }

    @GetMapping("/{messageId}")
    public BaseResponse<MqDlqRecord> detail(@PathVariable String messageId) {
        MqDlqRecord record = dlqCompensationService.getByMessageId(messageId);
        if (record == null) {
            return BaseResultUtils.error(ErrorCodeEnum.DLQ_RECORD_NOT_FOUND);
        }
        return BaseResultUtils.successOfData(record);
    }

    @PostMapping("/compensate/{messageId}")
    public BaseResponse<Void> compensate(@PathVariable String messageId) {
        ErrorCodeEnum result = dlqCompensationService.compensate(messageId);
        if (ErrorCodeEnum.SUCCESS == result) {
            return BaseResultUtils.success();
        }
        return BaseResultUtils.error(result);
    }
}
