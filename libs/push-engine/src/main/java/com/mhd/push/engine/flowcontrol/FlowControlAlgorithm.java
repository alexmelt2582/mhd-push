package com.mhd.push.engine.flowcontrol;

/**
 * 限流算法。
 * <p>
 * TOKEN_BUCKET 适合邮件、飞书这类允许短时突发的通知型渠道。
 * LEAKY_BUCKET 适合支付、清结算这类要求更接近匀速输出的通道。
 */
public enum FlowControlAlgorithm {
    /**
     * 适合邮件、飞书这类允许短时突发的通知型渠道。
     */
    TOKEN_BUCKET,
    /**
     * 适合支付、清结算这类要求更接近匀速输出的通道。
     */
    LEAKY_BUCKET
}