package com.mhd.push.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * 发送状态枚举
 *
 * @author zhao-hao-dong
 */
@Getter
@ToString
@AllArgsConstructor
public enum MsgPushState implements PowerfulEnum {
    SEND_PRE_CHECK_MODULE_SUCCESS(1010, "消息预检查--模块处理成功"),

    SEND_ASSEMBLE_MODULE_SUCCESS(1020, "消息组装--模块处理成功"),

    SEND_AFTER_CHECK_MODULE_SUCCESS(1030, "消息后置检查--模块处理成功"),

    SEND_MQ_MODULE_SUCCESS(1040, "消息投递MQ--模块处理成功"),
    SEND_MQ_MODULE_FAIL(1041, "消息投递MQ--模块处理失败"),

    RECEIVE_MQ_MODULE_SUCCESS(2010, "消息从MQ消费--模块处理成功"),

    DISCARD_MODULE_SUCCESS(2020, "消息丢弃--模块处理成功"),
    DISCARD_DISCARD(2021, "消息被丢弃"),

    SENSITIVE_MODULE_SUCCESS(2025, "消息敏感词匹配--模块处理成功"),
    SENSITIVE_MODULE_FAIL(2026, "消息敏感词匹配--模块处理失败"),

    SHIELD_MODULE_SUCCESS(2030, "消息屏蔽--模块处理成功"),
    SHIELD_NIGHT_SUCCESS(2031, "消息被夜间屏蔽"),
    SHIELD_NIGHT_NEXT_SEND_SUCCESS(2032, "消息被夜间屏蔽，次日9点发送"),

    DEDUPLICATION_MODULE_SUCCESS(2040, "消息去重--模块处理成功"),
    DEDUPLICATION_CONTENT_SUCCESS(2041, "消息被内容去重"),
    DEDUPLICATION_FREQUENCY_SUCCESS(2042, "消息被频次去重"),


    CHANNEL_ROUTE_SUCCESS(2050, "匹配到对应的渠道处理器，准备发送消息"),
    CHANNEL_ROUTE_FAIL(2051, "未找到对应的渠道处理器，无法发送消息"),

    /**
     * 下发成功（调用渠道接口成功）
     */
    SEND_SUCCESS(60, "消息下发成功"),
    /**
     * 下发结果待确认（调用第三方期间本地未完成最终确认，后续不自动重复发送）
     */
    SEND_PENDING_CONFIRM(65, "消息下发待确认"),
    /**
     * 下发失败（调用渠道接口失败）
     */
    SEND_FAIL(70, "消息发送失败，请稍后重试"),
    ;

    /**
     * 对外状态: 未投递。
     */
    public static final int DELIVERY_NOT_DELIVERED = 0;
    /**
     * 对外状态: 发送中。
     */
    public static final int DELIVERY_SENDING = 1;
    /**
     * 对外状态: 已发送。
     */
    public static final int DELIVERY_SUCCESS = 2;
    /**
     * 对外状态: 发送失败。
     */
    public static final int DELIVERY_FAIL = 3;

    private final Integer code;
    private final String description;

    /**
     * 根据状态码查找枚举。
     *
     * @param code 状态码
     * @return 匹配到的状态枚举，未匹配时返回 null
     */
    public static MsgPushState findByCode(Integer code) {
        // 空值直接返回，避免后续空指针判断分散在调用方。
        if (code == null) {
            return null;
        }
        // 顺序遍历枚举，当前状态数量较少，足以满足查询性能要求。
        for (MsgPushState value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 判断当前内部状态是否为对外成功态。
     *
     * @return true 表示对外已发送
     */
    public boolean isExternalSuccessState() {
        return this == SEND_SUCCESS;
    }

    /**
     * 判断当前内部状态是否为对外失败态。
     *
     * @return true 表示对外发送失败
     */
    public boolean isExternalFailureState() {
        return this == DISCARD_DISCARD
                || this == SHIELD_NIGHT_SUCCESS
                || this == SHIELD_NIGHT_NEXT_SEND_SUCCESS
                || this == DEDUPLICATION_CONTENT_SUCCESS
                || this == DEDUPLICATION_FREQUENCY_SUCCESS
                || this == SEND_FAIL;
    }

    /**
     * 将内部状态映射为对外投递状态。
     *
     * @return 对外投递状态码
     */
    public int toExternalDeliveryStatus() {
        // 成功态直接映射为“已发送”。
        if (isExternalSuccessState()) {
            return DELIVERY_SUCCESS;
        }
        // 丢弃、夜间屏蔽、去重、统一发送失败都视为“发送失败”。
        if (isExternalFailureState()) {
            return DELIVERY_FAIL;
        }
        // 其余中间态统一视为“发送中”。
        return DELIVERY_SENDING;
    }

    /**
     * 获取对外可展示的失败原因。
     *
     * @return 失败原因，非失败态返回空字符串
     */
    public String toExternalErrorMessage() {
        return isExternalFailureState() ? description : "";
    }
}
