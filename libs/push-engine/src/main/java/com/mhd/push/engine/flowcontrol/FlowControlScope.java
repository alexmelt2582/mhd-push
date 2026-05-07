package com.mhd.push.engine.flowcontrol;

/**
 * 限流粒度。
 */
public enum FlowControlScope {
    /**
     * 仅按渠道限流。
     */
    CHANNEL,
    /**
     * 按渠道 + 发送账号限流，适合“多个邮件账号、多个 webhook 各自独立额度”的场景。
     */
    CHANNEL_ACCOUNT,
    /**
     * 按渠道 + 账号 + 业务方限流，适合同账号下不同 tenant 独立额度。
     */
    CHANNEL_ACCOUNT_TENANT,
    /**
     * 按渠道 + 账号 + 实际 endpoint 限流，适合一个账号对象内部还区分真实 webhook 的场景。
     */
    ACCOUNT_ENDPOINT
}