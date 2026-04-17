package com.mhd.push.support.mq;

/**
 * 发送消息到消息队列
 *
 * @author zhao-hao-dong
 */
public interface SendMqService {
    /**
     * 发送消息
     */
    void send(String topic, String jsonValue, String tagId);

    /**
     * 发送消息（可携带顺序键）
     */
    default void send(String topic, String jsonValue, String tagId, String orderKey) {
        send(topic, jsonValue, tagId);
    }

    /**
     * 发送消息
     */
    void send(String topic, String jsonValue);
}
