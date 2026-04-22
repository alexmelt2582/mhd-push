package com.mhd.push.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * 发送类型枚举
 *
 * @author zhao-hao-dong
 */
@Getter
@ToString
@AllArgsConstructor
public enum SendTypeEnum {
    /**
     * 普通发送流程
     */
    SEND("send", "普通发送"),

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
