package com.mhd.push.domain.model.template;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模板变量定义。
 * <p>
 * 该模型用于描述模板中每个占位符的语义和校验规则。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateVariableDefinition {
    /**
     * 变量键，对应模板中的占位符名称，例如 orderId。
     */
    private String key;

    /**
     * 变量说明。
     */
    private String description;

    /**
     * 变量类型，例如 string、number。
     */
    private String type;

    /**
     * 是否必填。
     */
    private Boolean required;

    /**
     * 非必填变量未传入时的默认值，未配置时默认为空字符串。
     */
    private String defaultValue;
}