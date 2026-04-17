package com.mhd.push.support.constants;

/**
 * 消息队列常量
 *
 * @author zhao-hao-dong
 */
public interface MessageQueuePipeline {
    String EVENT_BUS = "eventBus";
    String REDIS = "redis";
    String KAFKA = "kafka";
    String ROCKET_MQ = "rocketMq";
    String RABBIT_MQ = "rabbitMq";
    String SPRING_EVENT_BUS = "springEventBus";
}
