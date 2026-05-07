package com.mhd.push.domain.model.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简单的埋点信息
 *
 * @author zhao-hao-dong
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SimpleAnchorInfo {
    /**
     * 具体点位
     */
    private int state;

    /**
     * 业务Id(数据追踪使用)
     * 生成逻辑参考 TaskInfoUtils
     */
    private Long businessId;

    /**
     * 当前节点阶段，例如 PRECHECK、ASSEMBLE、CHANNEL_SEND。
     */
    private String stage;

    /**
     * 当前节点结果，例如 PASS、FAIL、PENDING。
     */
    private String result;

    /**
     * 当前节点日志级别，例如 INFO、WARN、ERROR。
     */
    private String logLevel;

    /**
     * 节点详细描述，用于 admin 链路展示。
     */
    private String description;

    /**
     * 生成时间
     */
    private long timestamp;
}
