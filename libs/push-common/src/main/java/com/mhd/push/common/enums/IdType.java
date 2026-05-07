package com.mhd.push.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author zhao-hao-dong
 */
@Getter
@AllArgsConstructor
public enum IdType {
    /**
     * 手机号
     */
    PHONE(30, "phone"),
    /**
     * 邮件
     */
    EMAIL(50, "email"),
    ;
    private final Integer code;
    private final String description;
}
