package com.mhd.push.common.constant;

/**
 * MQ/DLQ 与接口幂等相关的 Redis Key 常量
 */
public final class MqRedisConstant {

    private MqRedisConstant() {
    }

    public static final String DLQ_RECORD_KEY_PREFIX = "mhd:mq:dlq:record:";
    public static final String DLQ_INDEX_KEY = "mhd:mq:dlq:index";

    public static final String IDEMPOTENCY_RESULT_KEY_PREFIX = "mhd:api:idempotency:result:";
    public static final String IDEMPOTENCY_LOCK_KEY_PREFIX = "mhd:api:idempotency:lock:";
}
