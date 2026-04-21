package com.mhd.push.web.api.action.send;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mhd.push.common.constant.CommonConstant;
import com.mhd.push.common.domain.TaskInfo;
import com.mhd.push.common.dto.model.ContentModel;
import com.mhd.push.common.enums.ChannelType;
import com.mhd.push.common.enums.ErrorCodeEnum;
import com.mhd.push.common.pipeline.BasicResultVO;
import com.mhd.push.common.pipeline.BusinessProcess;
import com.mhd.push.common.pipeline.ProcessContext;
import com.mhd.push.web.api.domain.MessageParam;
import com.mhd.push.web.api.domain.SendTaskModel;
import com.mhd.push.support.domain.entity.MessageTemplate;
import com.mhd.push.support.utils.ContentHolderUtil;
import com.mhd.push.support.utils.TaskInfoUtils;
import com.mhd.push.web.service.MessageTemplateCacheService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 拼装参数
 *
 * @author zhao-hao-dong

 */
@Slf4j
@Service
public class SendAssembleAction implements BusinessProcess<SendTaskModel> {

    private static final String LINK_NAME = "url";
    private static final String EXTRA_BUSINESS_OWNER = "businessOwner";
    private static final String EXTRA_DLQ_CALLBACK_URL = "dlqCallbackUrl";
    private static final String EXTRA_ORDER_KEY = "orderKey";

    @Resource
    private MessageTemplateCacheService messageTemplateCacheService;

    /**
     * 获取 contentModel，替换模板msgContent中占位符信息
     */
    private static ContentModel getContentModelValue(MessageTemplateCacheService.MessageTemplateSnapshot snapshot,
                                                     MessageParam messageParam) {
        MessageTemplate messageTemplate = snapshot.getTemplate();
        Map<String, String> variables = messageParam.getVariables();

        // 通过反射 组装出 contentModel
        ContentModel contentModel = ReflectUtil.newInstance(snapshot.getContentModelClass());
        for (Field field : snapshot.getContentFields()) {
            String originValue = snapshot.getTemplateContent().getString(field.getName());

            if (CharSequenceUtil.isNotBlank(originValue)) {
                String resultValue = ContentHolderUtil.replacePlaceHolder(originValue, variables);
                Object resultObj = JSONUtil.isJsonObj(resultValue) ? JSONUtil.toBean(resultValue, field.getType()) : resultValue;
                ReflectUtil.setFieldValue(contentModel, field, resultObj);
            }
        }

        // 如果 url 字段存在，则在url拼接对应的埋点参数
        String url = (String) ReflectUtil.getFieldValue(contentModel, LINK_NAME);
        if (CharSequenceUtil.isNotBlank(url)) {
            String resultUrl = TaskInfoUtils.generateUrl(url, messageTemplate.getId(), messageTemplate.getTemplateType());
            ReflectUtil.setFieldValue(contentModel, LINK_NAME, resultUrl);
        }
        return contentModel;
    }

    @Override
    public void process(ProcessContext<SendTaskModel> context) {
        SendTaskModel sendTaskModel = context.getProcessModel();
        Long messageTemplateId = sendTaskModel.getMessageTemplateId();

        try {
            MessageTemplateCacheService.MessageTemplateSnapshot snapshot = messageTemplateCacheService.getActiveTemplateSnapshot(messageTemplateId);
            MessageTemplate messageTemplate = snapshot == null ? null : snapshot.getTemplate();
            // 模板不存在或者已删除
            if (messageTemplate == null || messageTemplate.getIsDeleted().equals(CommonConstant.TRUE)) {
                context.setNeedBreak(true).setResponse(BasicResultVO.fail(ErrorCodeEnum.TEMPLATE_NOT_FOUND));
                return;
            }
            List<TaskInfo> taskInfos = assembleTaskInfo(sendTaskModel, snapshot);
            sendTaskModel.setTaskInfo(taskInfos);
        } catch (Exception e) {
            context.setNeedBreak(true).setResponse(BasicResultVO.fail(ErrorCodeEnum.SERVICE_ERROR));
            log.error("assemble task fail! templateId:{}, e:{}", messageTemplateId, ExceptionUtil.stacktraceToString(e));
        }
    }

    /**
     * 组装 TaskInfo 任务消息
     */
    private List<TaskInfo> assembleTaskInfo(SendTaskModel sendTaskModel, MessageTemplateCacheService.MessageTemplateSnapshot snapshot) {
        MessageTemplate messageTemplate = snapshot.getTemplate();
        List<MessageParam> messageParamList = sendTaskModel.getMessageParamList();
        List<TaskInfo> taskInfoList = new ArrayList<>();
        for (MessageParam messageParam : messageParamList) {
            Map<String, String> extra = Optional.ofNullable(messageParam.getExtra()).orElse(Collections.emptyMap());
            TaskInfo taskInfo = TaskInfo.builder()
                    .messageId(TaskInfoUtils.generateMessageId())
                    .bizId(messageParam.getBizId())
                    .messageTemplateId(messageTemplate.getId())
                    .businessId(TaskInfoUtils.generateBusinessId(messageTemplate.getId(), messageTemplate.getTemplateType()))
                    .receiver(new HashSet<>(Arrays.asList(messageParam.getReceiver().split(String.valueOf(StrPool.C_COMMA)))))
                    .idType(messageTemplate.getIdType())
                    .sendChannel(messageTemplate.getSendChannel())
                    .templateType(messageTemplate.getTemplateType())
                    .msgType(messageTemplate.getMsgType())
                    .shieldType(messageTemplate.getShieldType())
                    .sendAccount(messageTemplate.getSendAccount())
                    .businessOwner(extra.get(EXTRA_BUSINESS_OWNER))
                    .dlqCallbackUrl(extra.get(EXTRA_DLQ_CALLBACK_URL))
                    .orderKey(extra.get(EXTRA_ORDER_KEY))
                    .contentModel(getContentModelValue(snapshot, messageParam)).build();
            if (CharSequenceUtil.isBlank(taskInfo.getBizId())) {
                taskInfo.setBizId(taskInfo.getMessageId());
            }

            taskInfoList.add(taskInfo);
        }

        return taskInfoList;
    }
}
