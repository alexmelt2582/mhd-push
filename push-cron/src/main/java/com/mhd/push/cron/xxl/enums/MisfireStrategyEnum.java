package com.mhd.push.cron.xxl.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 调度过期策略
 *
 * @author zhao-hao-dong
 */
@AllArgsConstructor
@Getter
public enum MisfireStrategyEnum {
    DO_NOTHING("DO_NOTHING", "do nothing"),
    FIRE_ONCE_NOW("FIRE_ONCE_NOW", "fire once now");
    private final String name;
    private final String description;
}
