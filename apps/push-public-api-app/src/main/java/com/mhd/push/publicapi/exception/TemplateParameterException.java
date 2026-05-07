package com.mhd.push.publicapi.exception;

import lombok.Getter;

import java.util.List;
import java.util.StringJoiner;

/**
 * 模板参数校验异常。
 */
@Getter
public class TemplateParameterException extends RuntimeException {
    /**
     *  返回缺失的必填变量。
     */
    private final List<String> missingRequiredKeys;

    /**
     * 使用缺失的必填变量集合构建异常。
     *
     * @param missingRequiredKeys 缺失的必填变量
     */
    public TemplateParameterException(List<String> missingRequiredKeys) {
        super(buildMessage(missingRequiredKeys));
        this.missingRequiredKeys = missingRequiredKeys;
    }

    private static String buildMessage(List<String> missingRequiredKeys) {
        StringJoiner joiner = new StringJoiner(", ");
        for (String missingRequiredKey : missingRequiredKeys) {
            joiner.add(missingRequiredKey);
        }
        return "模板缺少必填参数: " + joiner + "。请检查模板变量定义后重新提交。";
    }
}