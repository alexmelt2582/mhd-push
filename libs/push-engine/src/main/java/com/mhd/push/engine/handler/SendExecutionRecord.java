package com.mhd.push.engine.handler;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单条消息在本地发送侧的执行台账。
 * <p>
 * 这份记录只负责解决“本地已经打到第三方，但结果尚未安全落盘”时的防重复发送问题，
 * 不承担最终审计表职责，所以保存在 Redis 中并设置 TTL。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendExecutionRecord {
    /**
     * Redis 主键，运维确认接口通过它定位记录。
     */
    private String executionKey;

    /**
     * 当前发送状态：SENDING / SUCCESS / FAIL / PENDING_CONFIRM。
     */
    private String status;

    /**
     * 业务侧的消息唯一标识。
     */
    private String traceId;

    private String businessOwner;
    private String orderingKey;

    /**
     * 渠道及账号维度。
     */
    private Integer channelCode;
    private Integer sendAccount;

    /**
     * 接收者明文摘要仅供运维排查，不参与幂等判断。
     */
    private String receiverSummary;

    /**
     * 接收者指纹参与执行记录唯一键计算，避免同 messageId 不同接收者互相覆盖。
     */
    private String receiverFingerprint;

    /**
     * 创建与最近更新时间戳。
     */
    private Long createdAt;
    private Long updatedAt;
}