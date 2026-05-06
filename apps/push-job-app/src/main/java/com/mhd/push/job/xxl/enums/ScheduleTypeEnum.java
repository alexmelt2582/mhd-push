package com.mhd.push.job.xxl.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 调度类型
 *
 * @author zhao-hao-dong
 */
@AllArgsConstructor
@Getter
public enum ScheduleTypeEnum {
    NONE("NONE", "NONE"),
    CRON("CRON", "schedule by cron"),
    FIX_RATE("FIX_RATE", "schedule by fixed rate (in seconds)");
    private final String name;
    private final String description;
}
