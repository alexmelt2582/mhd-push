package com.mhd.push.web.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * @author zhao-hao-dong

 */
@Getter
@ToString
@AllArgsConstructor
public enum BusinessCode {
    /**
     * 普通发送流程
     */
    COMMON_SEND("send", "普通发送"),

    /**
     * 撤回流程
     */
    RECALL("recall", "撤回消息");

    /**
     * code 代表责任链的模板
     */
    private final String code;
    /**
     * description 代表类型描述
     */
    private final String description;
}
