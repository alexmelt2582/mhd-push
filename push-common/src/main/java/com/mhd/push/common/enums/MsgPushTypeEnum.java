package com.mhd.push.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务类型枚举
 *
 * @author zhao-hao-dong
 */
@Getter
@AllArgsConstructor
public enum MsgPushTypeEnum {
    SEND("Send", "普通消息发送")
    ;
    /**
     * 业务类型
     */
    private final String code;

    /**
     * 描述
     */
    private final String description;
}
