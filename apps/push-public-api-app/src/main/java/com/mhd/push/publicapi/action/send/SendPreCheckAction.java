package com.mhd.push.publicapi.action.send;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.text.StrPool;
import com.mhd.push.common.constant.GlobalConstant;
import com.mhd.push.common.pipeline.BasicResultVO;
import com.mhd.push.common.pipeline.BusinessProcess;
import com.mhd.push.common.pipeline.ProcessContext;
import com.mhd.push.publicapi.domain.MessageParam;
import com.mhd.push.publicapi.domain.SendTaskModel;
import com.mhd.push.publicapi.exception.ClientErrorCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 发送前置校验动作。
 * <p>
 * 负责在进入模板组装前校验模板 ID、接收者列表以及单次请求的接收者规模。
 * </p>
 */
@Slf4j
@Service
public class SendPreCheckAction implements BusinessProcess<SendTaskModel> {

    /**
     * 执行发送前基础参数校验。
     *
     * @param context 流程上下文
     */
    @Override
    public void process(ProcessContext<SendTaskModel> context) {
        SendTaskModel sendTaskModel = context.getProcessModel();

        // 1. 检查模板ID和参数信息是否存在
        Long templateId = sendTaskModel.getTemplateId();
        List<MessageParam> messageParamList = sendTaskModel.getMessageParamList();
        if (Objects.isNull(templateId) || CollUtil.isEmpty(messageParamList)) {
            context.setNeedBreak(true).setResponse(BasicResultVO.fail(ClientErrorCodeEnum.CLIENT_SEND_BAD_PARAMETERS, "模板ID或参数列表为空"));
            return;
        }

        // 2. 过滤掉接收者为空的数据，避免进入后续组装逻辑。
        List<MessageParam> resultMessageParamList = messageParamList.stream().filter(messageParam -> !CharSequenceUtil.isBlank(messageParam.getReceiver())).collect(Collectors.toList());
        if (CollUtil.isEmpty(resultMessageParamList)) {
            context.setNeedBreak(true).setResponse(BasicResultVO.fail(ClientErrorCodeEnum.CLIENT_SEND_BAD_PARAMETERS, "含接受者的参数列表为空"));
            return;
        }

        // 3. 控制单条参数中的接收者人数，避免超出系统约定批量规模。
        if (resultMessageParamList.stream().anyMatch(messageParam -> messageParam.getReceiver().split(StrPool.COMMA).length > GlobalConstant.BATCH_RECEIVER_SIZE)) {
            // 单次接收者规模超限时保留内部日志，外部仍返回统一业务错误。
            context.setNeedBreak(true).setResponse(BasicResultVO.fail(ClientErrorCodeEnum.CLIENT_SEND_BAD_PARAMETERS, "传入的接收者大于"+ GlobalConstant.BATCH_RECEIVER_SIZE +"个"));
            return;
        }
        // 4. 回写过滤后的参数列表，供后续 action 使用。
        sendTaskModel.setMessageParamList(resultMessageParamList);
    }
}
