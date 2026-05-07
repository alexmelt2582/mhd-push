package com.mhd.push.engine.flowcontrol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Handler 侧传给限流模块的运行参数。
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FlowControlParam {

    /**
     * 默认 QPS。
     * 当配置中心没有下发生产规则时，使用这个值作为兜底限流。
     */
    protected Double rateInitValue;

    /**
     * 本次发送的令牌计算方式。
     * 请求级按 1 个 permit 计算，按接收人数限流时按 receiver.size() 计算。
     */
    protected RateLimitStrategy rateLimitStrategy;
}
