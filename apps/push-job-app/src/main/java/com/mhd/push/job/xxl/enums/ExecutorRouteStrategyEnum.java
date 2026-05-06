package com.mhd.push.job.xxl.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 路由策略
 *
 * @author zhao-hao-dong
 */
@AllArgsConstructor
@Getter
public enum ExecutorRouteStrategyEnum {
    FIRST("FIRST", "第一个"),
    LAST("LAST", "最后一个"),
    ROUND("ROUND", "轮询"),
    RANDOM("RANDOM", "随机"),
    CONSISTENT_HASH("CONSISTENT_HASH", "一致性 HASH"),
    LEAST_FREQUENTLY_USED("LEAST_FREQUENTLY_USED", "最不经常使用"),
    LEAST_RECENTLY_USED("LEAST_RECENTLY_USED", "最近最久未使用"),
    FAILOVER("FAILOVER", "故障转移"),
    BUSYOVER("BUSYOVER", "忙碌转移"),
    SHARDING_BROADCAST("SHARDING_BROADCAST", "分片广播");
    private final String name;
    private final String description;
}
