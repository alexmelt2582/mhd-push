package com.mhd.push.engine.flowcontrol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单条生产级限流规则。
 * <p>
 * qps 负责短周期速率控制；perMinute / perDay 负责更大窗口的配额控制。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowControlRule {
    /**
     * 使用哪种限流算法。
     */
    private FlowControlAlgorithm algorithm;
    /**
     * 限流粒度。
     */
    private FlowControlScope scope;
    /**
     * 每秒允许的请求数或令牌数。
     */
    private Double qps;
    /**
     * 每分钟配额。
     */
    private Integer perMinute;
    /**
     * 每日配额。
     */
    private Integer perDay;
}