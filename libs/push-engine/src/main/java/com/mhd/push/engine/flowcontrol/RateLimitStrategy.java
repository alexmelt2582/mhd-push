package com.mhd.push.engine.flowcontrol;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * 限流策略
 *
 * @author zhao-hao-dong
 */
@Getter
@ToString
@AllArgsConstructor
public enum RateLimitStrategy {
    /**
     * 根据真实请求数限流。
     */
    REQUEST_RATE_LIMIT(10, "根据真实请求数限流"),
    /**
     * 根据发送用户数限流。
     * 一次群发如果包含 100 个接收者，就会一次性消耗 100 个 permit。
     */
    SEND_USER_NUM_RATE_LIMIT(20, "根据发送用户数限流"),
    ;

    private final Integer code;
    private final String description;
}
