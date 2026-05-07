package com.mhd.push.domain.model.template;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 模板内容定义。
 * <p>
 * 该模型对应 message_template.msg_content 的结构化内容，区分正文内容、正文变量定义以及额外参数定义。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateContentDefinition {
    /**
     * 模板正文。
     */
    private String content;

    /**
     * 正文中占位符的定义列表。
     */
    @JSONField(name = "content_params_schema")
    private List<TemplateVariableDefinition> contentParamsSchema;

    /**
     * 额外参数定义列表，用于组装渠道专属字段，例如 title、url。
     */
    @JSONField(name = "extra_params_schema")
    private List<TemplateVariableDefinition> extraParamsSchema;
}