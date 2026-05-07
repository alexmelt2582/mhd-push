package com.mhd.push.common.constant;

/**
 * 消息队列实现类型常量。
 */
public interface MessageQueuePipeline {

    String EVENT_BUS = "eventBus";

    String SPRING_EVENT_BUS = "springEventBus";

    String REDIS = "redis";

    String ROCKET_MQ = "rocketMq";

    String RABBIT_MQ = "rabbitMq";

    String KAFKA = "kafka";
}