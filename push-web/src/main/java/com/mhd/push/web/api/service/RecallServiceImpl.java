//package com.mhd.push.web.api.service;
//
//import com.mhd.push.common.enums.ErrorCodeEnum;
//import com.mhd.push.common.pipeline.ProcessContext;
//import com.mhd.push.common.pipeline.ProcessController;
//import com.mhd.push.web.api.domain.RecallTaskModel;
//import com.mhd.push.web.api.domain.SendRequest;
//import com.mhd.push.web.api.domain.SendResponse;
//import org.apache.commons.lang3.ObjectUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.stereotype.Service;
//
//
///**
// * 撤回接口
// *
// * @author 3y
// */
//@Service
//public class RecallServiceImpl implements RecallService {
//    @Autowired
//    @Qualifier("apiProcessController")
//    private ProcessController processController;
//
//    @Override
//    public SendResponse recall(SendRequest sendRequest) {
//
//        if (ObjectUtils.isEmpty(sendRequest)) {
//            return new SendResponse(ErrorCodeEnum.CLIENT_BAD_PARAMETERS.getCode(), RespStatusEnum.CLIENT_BAD_PARAMETERS.getMsg(), null);
//        }
//        RecallTaskModel recallTaskModel = RecallTaskModel.builder().messageTemplateId(sendRequest.getMessageTemplateId()).recallMessageId(sendRequest.getRecallMessageIds()).build();
//        ProcessContext context = ProcessContext.builder().code(sendRequest.getCode()).processModel(recallTaskModel).needBreak(false).response(BasicResultVO.success()).build();
//
//        ProcessContext process = processController.process(context);
//        return new SendResponse(process.getResponse().getStatus(), process.getResponse().getMsg(), null);
//    }
//}
