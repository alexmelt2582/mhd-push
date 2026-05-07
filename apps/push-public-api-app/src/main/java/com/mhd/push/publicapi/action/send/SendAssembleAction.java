package com.mhd.push.publicapi.action.send;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.json.JSONUtil;
import com.mhd.push.common.constant.CommonConstant;
import com.mhd.push.common.dto.model.ContentModel;
import com.mhd.push.common.enums.MsgPushState;
import com.mhd.push.common.enums.SendTypeEnum;
import com.mhd.push.common.log.LogRecord;
import com.mhd.push.common.pipeline.BasicResultVO;
import com.mhd.push.common.pipeline.BusinessProcess;
import com.mhd.push.common.pipeline.ProcessContext;
import com.mhd.push.common.utils.ContentHolderUtil;
import com.mhd.push.common.utils.TaskInfoUtils;
import com.mhd.push.domain.model.task.TaskInfo;
import com.mhd.push.domain.model.template.TemplateContentDefinition;
import com.mhd.push.domain.model.template.TemplateVariableDefinition;
import com.mhd.push.infra.persistence.entity.MessageTemplate;
import com.mhd.push.infra.service.MessageTemplateCacheService;
import com.mhd.push.infra.utils.LogUtils;
import com.mhd.push.publicapi.domain.MessageParam;
import com.mhd.push.publicapi.domain.SendTaskModel;
import com.mhd.push.publicapi.exception.ClientErrorCodeEnum;
import com.mhd.push.publicapi.exception.TemplateParameterException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 发送组装动作。
 * <p>
 * 该动作负责读取模板快照、校验模板变量并构造发送任务。
 * </p>
 */
@Slf4j
@Service
public class SendAssembleAction implements BusinessProcess<SendTaskModel> {

    private static final String LINK_NAME = "url";
    private static final String EXTRA_BUSINESS_OWNER = "businessOwner";
    private static final String EXTRA_CALLBACK_URL = "callbackUrl";

    @Resource
    private MessageTemplateCacheService messageTemplateCacheService;
    @Resource
    private LogUtils logUtils;

    /**
     * 发送前的组装流程：读取模板快照并构造任务列表。
     *
     * @param context 流程上下文
     */
    @Override
    public void process(ProcessContext<SendTaskModel> context) {
        SendTaskModel sendTaskModel = context.getProcessModel();

        Long templateId = sendTaskModel.getTemplateId();
        try {
            // 1. 读取模板快照，确保本次请求使用同一份模板视图完成校验与填充。
            MessageTemplateCacheService.MessageTemplateSnapshot snapshot = messageTemplateCacheService.getActiveTemplateSnapshot(templateId);
            MessageTemplate messageTemplate = snapshot == null ? null : snapshot.getTemplate();

            // 2. 模板不存在或已删除时直接终止流程。
            if (messageTemplate == null || messageTemplate.getIsDeleted().equals(CommonConstant.TRUE)) {
                context.setNeedBreak(true).setResponse(BasicResultVO.fail(ClientErrorCodeEnum.CLIENT_SEND_TEMPLATE_NOT_FOUND));
                return;
            }

            // 3. 模板有效时组装发送任务。
            List<TaskInfo> taskInfos = assembleTaskInfo(sendTaskModel, snapshot);
            sendTaskModel.setTaskInfo(taskInfos);
            for (TaskInfo taskInfo : taskInfos) {
                LogRecord preCheckLogRecord = LogRecord.build(SendTypeEnum.SEND, taskInfo, MsgPushState.SEND_PRE_CHECK_MODULE_SUCCESS);
                LogRecord assembleLogRecord = LogRecord.build(SendTypeEnum.SEND, taskInfo, MsgPushState.SEND_ASSEMBLE_MODULE_SUCCESS);
                logUtils.print(preCheckLogRecord);
                logUtils.print(assembleLogRecord);
            }
        } catch (TemplateParameterException ex) {
            context.setNeedBreak(true).setResponse(BasicResultVO.fail(ClientErrorCodeEnum.CLIENT_SEND_TEMPLATE_PARAM_FAIL, ex.getMessage()));
        } catch (Exception e) {
            context.setNeedBreak(true).setResponse(BasicResultVO.fail(ClientErrorCodeEnum.CLIENT_SEND_FAIL));
            log.error("assemble task fail! templateId:{}, e:{}", templateId, ExceptionUtil.stacktraceToString(e));
        }
    }

    /**
     * 组装 TaskInfo 任务消息。
     *
     * @param sendTaskModel 发送模型
     * @param snapshot      模板快照
     * @return 已填充的任务列表
     */
    private List<TaskInfo> assembleTaskInfo(SendTaskModel sendTaskModel, MessageTemplateCacheService.MessageTemplateSnapshot snapshot) {
        MessageTemplate messageTemplate = snapshot.getTemplate();
        List<MessageParam> messageParamList = sendTaskModel.getMessageParamList();
        List<TaskInfo> taskInfoList = new ArrayList<>();
        for (MessageParam messageParam : messageParamList) {
            // 1. 优先按模板定义校验必填变量，避免缺参时落成模糊服务异常。
            validateTemplateParams(snapshot, messageParam);

            // 2. 先取每条消息级别扩展参数，保证批量下可按条定制。
            Map<String, String> extra = Optional.ofNullable(messageParam.getExtra()).orElse(Collections.emptyMap());

            // 3. 回调地址支持两级来源：messageParam.extra.callbackUrl 优先，全局 callbackUrl 兜底。
            String callbackUrl = CharSequenceUtil.isNotBlank(extra.get(EXTRA_CALLBACK_URL))
                    ? extra.get(EXTRA_CALLBACK_URL)
                    : sendTaskModel.getCallbackUrl();

            // 4. 将模板元数据和本次变量渲染为最终任务。
            TaskInfo taskInfo = TaskInfo.builder()
                    .traceId(TaskInfoUtils.getTraceId())
                    .templateId(messageTemplate.getId())
                    .receiver(new HashSet<>(Arrays.asList(messageParam.getReceiver().split(String.valueOf(StrPool.C_COMMA)))))
                    .idType(messageTemplate.getIdType())
                    .sendChannel(messageTemplate.getSendChannel())
                    .templateType(messageTemplate.getTemplateType())
                    .msgType(messageTemplate.getMsgType())
                    .shieldType(messageTemplate.getShieldType())
                    .sendAccount(messageTemplate.getSendAccount())
                    .businessOwner(extra.get(EXTRA_BUSINESS_OWNER))
                    .orderingKey(sendTaskModel.getOrderingKey())
                    .callbackUrl(callbackUrl)
                    .contentModel(getContentModelValue(snapshot, messageParam)).build();
            taskInfoList.add(taskInfo);
        }
        return taskInfoList;
    }

    /**
     * 获取渲染后的内容模型。
     *
     * @param snapshot     模板快照
     * @param messageParam 本次消息参数
     * @return 已替换占位符的内容模型
     */
    private static ContentModel getContentModelValue(MessageTemplateCacheService.MessageTemplateSnapshot snapshot,
                                                     MessageParam messageParam) {
        MessageTemplate messageTemplate = snapshot.getTemplate();
        Map<String, String> templateParams = Optional.ofNullable(messageParam.getTemplateParams()).orElse(Collections.emptyMap());
        TemplateContentDefinition contentDefinition = snapshot.getTemplateContentDefinition();

        // 1. 通过反射构造渠道对应的内容模型。
        ContentModel contentModel = ReflectUtil.newInstance(snapshot.getContentModelClass());
        // 2. 渲染模板正文，并注入当前渠道的正文承载字段。
        Field contentField = snapshot.getContentFieldMap().get(snapshot.getContentCarrierFieldName());
        if (contentField != null && CharSequenceUtil.isNotBlank(contentDefinition.getContent())) {
            String renderedContent = ContentHolderUtil.replacePlaceHolder(contentDefinition.getContent(), placeholderName ->
                    resolveTemplateParamValue(placeholderName, templateParams, snapshot.getContentParamDefinitionMap()));
            applyFieldValue(contentModel, contentField, renderedContent);
        }

        // 3. 根据 extra_params_schema 组装渠道附加字段。
        for (TemplateVariableDefinition definition : snapshot.getExtraParamDefinitionMap().values()) {
            Field targetField = snapshot.getContentFieldMap().get(definition.getKey());
            if (targetField == null) {
                throw new IllegalStateException("Field not found in content model: " + definition.getKey());
            }
            String fieldValue = resolveTemplateParamValue(definition.getKey(), templateParams, snapshot.getExtraParamDefinitionMap());
            applyFieldValue(contentModel, targetField, fieldValue);
        }

        // 4. 如果 url 字段存在，则在 url 末尾拼接追踪参数。
        String url = (String) ReflectUtil.getFieldValue(contentModel, LINK_NAME);
        if (CharSequenceUtil.isNotBlank(url)) {
            String resultUrl = TaskInfoUtils.generateUrl(url, messageTemplate.getId(), messageTemplate.getTemplateType());
            ReflectUtil.setFieldValue(contentModel, LINK_NAME, resultUrl);
        }
        return contentModel;
    }

    /**
     * 校验模板参数是否满足当前模板要求。
     *
     * @param snapshot     模板快照
     * @param messageParam 本次消息参数
     */
    private void validateTemplateParams(MessageTemplateCacheService.MessageTemplateSnapshot snapshot, MessageParam messageParam) {
        Map<String, String> templateParams = Optional.ofNullable(messageParam.getTemplateParams()).orElse(Collections.emptyMap());
        List<String> missingRequiredKeys = new ArrayList<>();
        // 1. 先校验正文占位符是否满足要求。
        for (String placeholder : snapshot.getContentPlaceholders()) {
            TemplateVariableDefinition definition = snapshot.getContentParamDefinitionMap().get(placeholder);
            boolean required = definition == null || Boolean.TRUE.equals(definition.getRequired());
            if (required && CharSequenceUtil.isBlank(templateParams.get(placeholder))) {
                missingRequiredKeys.add(placeholder);
            }
        }

        // 2. 再校验额外参数 schema 中声明的必填字段。
        for (TemplateVariableDefinition definition : snapshot.getExtraParamDefinitionMap().values()) {
            if (Boolean.TRUE.equals(definition.getRequired()) && CharSequenceUtil.isBlank(templateParams.get(definition.getKey()))) {
                missingRequiredKeys.add(definition.getKey());
            }
        }
        if (!missingRequiredKeys.isEmpty()) {
            throw new TemplateParameterException(missingRequiredKeys);
        }
    }

    /**
     * 根据模板参数定义解析单个参数值。
     *
     * @param key            参数键
     * @param templateParams 用户传入参数
     * @param definitionMap  参数定义映射
     * @return 最终使用的参数值
     */
    private static String resolveTemplateParamValue(String key, Map<String, String> templateParams,
                                                    Map<String, TemplateVariableDefinition> definitionMap) {
        String value = templateParams.get(key);
        if (CharSequenceUtil.isNotBlank(value)) {
            return value;
        }
        TemplateVariableDefinition definition = definitionMap.get(key);
        if (definition != null && !Boolean.TRUE.equals(definition.getRequired())) {
            return CharSequenceUtil.nullToEmpty(definition.getDefaultValue());
        }
        throw new TemplateParameterException(List.of(key));
    }

    /**
     * 将字符串值写入 ContentModel 指定字段。
     *
     * @param contentModel 内容模型
     * @param field        目标字段
     * @param fieldValue   字段值
     */
    private static void applyFieldValue(ContentModel contentModel, Field field, String fieldValue) {
        Object convertedValue;
        if (field.getType().equals(String.class)) {
            convertedValue = fieldValue;
        } else if (Map.class.isAssignableFrom(field.getType())) {
            convertedValue = JSONUtil.isJsonObj(fieldValue)
                    ? JSONUtil.toBean(fieldValue, LinkedHashMap.class)
                    : Collections.singletonMap("content", fieldValue);
        } else if (CharSequenceUtil.isBlank(fieldValue)) {
            convertedValue = null;
        } else {
            convertedValue = Convert.convert(field.getType(), fieldValue);
        }
        ReflectUtil.setFieldValue(contentModel, field, convertedValue);
    }
}
