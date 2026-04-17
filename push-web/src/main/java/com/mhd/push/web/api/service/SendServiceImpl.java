package com.mhd.push.web.api.service;

import cn.monitor4all.logRecord.annotation.OperationLog;
import com.mhd.push.common.domain.SimpleTaskInfo;
import com.mhd.push.common.enums.ErrorCodeEnum;
import com.mhd.push.common.pipeline.BasicResultVO;
import com.mhd.push.common.pipeline.ProcessContext;
import com.mhd.push.common.pipeline.ProcessController;
import com.mhd.push.web.api.domain.BatchSendRequest;
import com.mhd.push.web.api.domain.SendResponse;
import com.mhd.push.web.api.domain.SendRequest;
import com.mhd.push.web.api.domain.SendTaskModel;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 发送接口
 *
 * @author zhao-hao-dong

 */
@Service
public class SendServiceImpl implements SendService {
    @Autowired
    @Qualifier("apiProcessController")
    private ProcessController processController;

    //@Override
    //public BaseResponse<String> send(SendRequest sendRequest) {
    //    if (Objects.isNull(sendRequest)) {
    //        return BaseResultUtils.error(ErrorCodeEnum.CLIENT_BAD_PARAMETERS);
    //    }
    //    SendTaskModel sendTaskModel = SendTaskModel.builder()
    //            .messageTemplateId(sendRequest.getMessageTemplateId())
    //            .messageParamList(Collections.singletonList(sendRequest.getMessageParam()))
    //            .build();
    //
    //    ProcessContext context = ProcessContext.builder()
    //            .code(sendRequest.getCode())
    //            .processModel(sendTaskModel)
    //            .needBreak(false)
    //            .processResp(BaseResultUtils.success()).build();
    //
    //    ProcessContext process = processController.process(context);
    //    BaseResponse processResp = process.getProcessResp();
    //    if(ErrorCodeEnum.SUCCESS.getCode().equals(processResp.getCode())) {
    //        return BaseResultUtils.successOfData(process.getBizId());
    //    }else {
    //        return processResp;
    //    }
    //}
    //
    //@Override
    //public BaseResponse<List<SendResponse>> batchSend(BatchSendRequest batchSendRequest) {
    //    if (Objects.isNull(batchSendRequest)) {
    //        return BaseResultUtils.error(ErrorCodeEnum.CLIENT_BAD_PARAMETERS);
    //    }
    //    List<SendResponse> res = new ArrayList<>();
    //
    //    SendTaskModel sendTaskModel = SendTaskModel.builder()
    //            .messageTemplateId(batchSendRequest.getMessageTemplateId())
    //            .messageParamList(batchSendRequest.getMessageParamList())
    //            .build();
    //
    //    ProcessContext context = ProcessContext.builder()
    //            .code(batchSendRequest.getCode())
    //            .processModel(sendTaskModel)
    //            .needBreak(false)
    //            .processResp(BaseResultUtils.success()).build();
    //
    //    ProcessContext process = processController.process(context);
    //    return BaseResultUtils.successOfData(res);
    //}

    @Override
    @OperationLog(bizType = "SendService#send", bizId = "#sendRequest.messageTemplateId", msg = "#sendRequest")
    public SendResponse send(SendRequest sendRequest) {
        if (ObjectUtils.isEmpty(sendRequest)) {
            return new SendResponse(ErrorCodeEnum.CLIENT_BAD_PARAMETERS.getCode(), ErrorCodeEnum.CLIENT_BAD_PARAMETERS.getMessage(), null);
        }

        SendTaskModel sendTaskModel = SendTaskModel.builder()
                .messageTemplateId(sendRequest.getMessageTemplateId())
                .messageParamList(Collections.singletonList(sendRequest.getMessageParam()))
                .build();

        ProcessContext context = ProcessContext.builder()
                .code(sendRequest.getCode())
                .processModel(sendTaskModel)
                .needBreak(false)
                .response(BasicResultVO.success()).build();

        ProcessContext process = processController.process(context);

        return new SendResponse(process.getResponse().getStatus(), process.getResponse().getMsg(), (List<SimpleTaskInfo>) process.getResponse().getData());
    }

    @Override
    @OperationLog(bizType = "SendService#batchSend", bizId = "#batchSendRequest.messageTemplateId", msg = "#batchSendRequest")
    public SendResponse batchSend(BatchSendRequest batchSendRequest) {
        if (ObjectUtils.isEmpty(batchSendRequest)) {
            return new SendResponse(ErrorCodeEnum.CLIENT_BAD_PARAMETERS.getCode(), ErrorCodeEnum.CLIENT_BAD_PARAMETERS.getMessage(), null);
        }

        SendTaskModel sendTaskModel = SendTaskModel.builder()
                .messageTemplateId(batchSendRequest.getMessageTemplateId())
                .messageParamList(batchSendRequest.getMessageParamList())
                .build();

        ProcessContext context = ProcessContext.builder()
                .code(batchSendRequest.getCode())
                .processModel(sendTaskModel)
                .needBreak(false)
                .response(BasicResultVO.success()).build();

        ProcessContext process = processController.process(context);

        return new SendResponse(process.getResponse().getStatus(), process.getResponse().getMsg(), (List<SimpleTaskInfo>) process.getResponse().getData());
    }
}
