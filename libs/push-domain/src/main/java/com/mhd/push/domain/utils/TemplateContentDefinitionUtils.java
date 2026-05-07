package com.mhd.push.domain.utils;

import com.alibaba.fastjson2.JSON;
import com.mhd.push.common.utils.ContentHolderUtil;
import com.mhd.push.domain.model.template.TemplateContentDefinition;
import com.mhd.push.domain.model.template.TemplateVariableDefinition;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 模板内容定义工具。
 */
public final class TemplateContentDefinitionUtils {

    private TemplateContentDefinitionUtils() {
    }

    /**
     * 解析 msgContent 字段中的模板内容定义。
     *
     * @param msgContent 模板内容 JSON
     * @return 解析后的模板定义
     */
    public static TemplateContentDefinition parse(String msgContent) {
        if (!StringUtils.hasText(msgContent)) {
            return TemplateContentDefinition.builder()
                    .content("")
                    .contentParamsSchema(Collections.emptyList())
                    .extraParamsSchema(Collections.emptyList())
                    .build();
        }
        TemplateContentDefinition definition = JSON.parseObject(msgContent, TemplateContentDefinition.class);
        if (definition == null) {
            return TemplateContentDefinition.builder()
                    .content("")
                    .contentParamsSchema(Collections.emptyList())
                    .extraParamsSchema(Collections.emptyList())
                    .build();
        }
        if (definition.getContentParamsSchema() == null) {
            definition.setContentParamsSchema(Collections.emptyList());
        }
        if (definition.getExtraParamsSchema() == null) {
            definition.setExtraParamsSchema(Collections.emptyList());
        }
        return definition;
    }

    /**
     * 序列化模板内容定义。
     *
     * @param definition 模板内容定义
     * @return JSON 文本
     */
    public static String toJson(TemplateContentDefinition definition) {
        if (definition == null) {
            return null;
        }
        return JSON.toJSONString(definition);
    }

    /**
     * 将变量定义列表按 key 建立索引。
     *
     * @param definitions 变量定义列表
     * @return 按 key 索引的变量定义
     */
    public static Map<String, TemplateVariableDefinition> indexDefinitions(List<TemplateVariableDefinition> definitions) {
        if (definitions == null || definitions.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, TemplateVariableDefinition> definitionMap = new LinkedHashMap<>();
        for (TemplateVariableDefinition definition : definitions) {
            if (definition == null || !StringUtils.hasText(definition.getKey())) {
                continue;
            }
            definitionMap.put(definition.getKey(), definition);
        }
        return definitionMap;
    }

    /**
     * 合并正文变量定义和额外参数定义。
     *
     * @param definition 模板内容定义
     * @return 合并后的变量定义列表
     */
    public static List<TemplateVariableDefinition> mergeDefinitions(TemplateContentDefinition definition) {
        if (definition == null) {
            return Collections.emptyList();
        }
        Map<String, TemplateVariableDefinition> merged = new LinkedHashMap<>();
        merged.putAll(indexDefinitions(definition.getContentParamsSchema()));
        merged.putAll(indexDefinitions(definition.getExtraParamsSchema()));
        return new ArrayList<>(merged.values());
    }

    /**
     * 提取正文中实际使用到的占位符。
     *
     * @param definition 模板内容定义
     * @return 占位符集合
     */
    public static Set<String> extractContentPlaceholders(TemplateContentDefinition definition) {
        if (definition == null) {
            return Collections.emptySet();
        }
        return new LinkedHashSet<>(ContentHolderUtil.extractPlaceHolders(definition.getContent()));
    }
}