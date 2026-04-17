package com.mhd.push.cron.xxl.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * GlueTyp 类型（默认BEAN)
 *
 * @author zhao-hao-dong
 */
@AllArgsConstructor
@Getter
public enum GlueTypeEnum {
    BEAN("BEAN","BEAN"),
    GLUE_GROOVY("GLUE_GROOVY","GLUE_GROOVY"),
    GLUE_SHELL("GLUE_SHELL","GLUE_SHELL"),
    GLUE_PYTHON("GLUE_PYTHON","GLUE_PYTHON"),
    GLUE_PHP("GLUE_PHP","GLUE_PHP"),
    GLUE_NODEJS("GLUE_NODEJS","GLUE_NODEJS"),
    GLUE_POWERSHELL("GLUE_POWERSHELL","GLUE_POWERSHELL"),
    ;
    private final String name;
    private final String description;
}
