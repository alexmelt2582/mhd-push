package com.mhd.push.cron.xxl.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 阻塞处理策略
 *
 * @author zhao-hao-dong
 **/
@AllArgsConstructor
@Getter
public enum ExecutorBlockStrategyEnum {
    SERIAL_EXECUTION("SERIAL_EXECUTION","单机串行"),
    DISCARD_LATER("DISCARD_LATER","丢弃后续调度"),
    COVER_EARLY("COVER_EARLY","覆盖之前调度")
    ;
    private final String name;
    private final String description;
}
