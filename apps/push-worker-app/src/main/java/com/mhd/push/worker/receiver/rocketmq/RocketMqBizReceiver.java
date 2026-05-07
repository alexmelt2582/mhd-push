package com.mhd.push.worker.receiver.rocketmq;

import com.mhd.push.common.constant.MessageQueuePipeline;
import com.mhd.push.engine.receiver.MessageReceiver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 普通并发消费通道（非有序业务默认走这里）
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "mhd.mq.pipeline", havingValue = MessageQueuePipeline.ROCKET_MQ)
@RocketMQMessageListener(topic = "${mhd.mq.topic.send}",
        consumerGroup = "${mhd.mq.rocketmq.consumer.group.send}",
        selectorType = SelectorType.TAG,
    selectorExpression = "${mhd.mq.tagId.value}",
    maxReconsumeTimes = 3)
public class RocketMqBizReceiver implements RocketMQListener<MessageExt>, MessageReceiver {

    @Resource
    private RocketMqConsumeService rocketMqConsumeService;

    @Override
    public void onMessage(MessageExt messageExt) {
        rocketMqConsumeService.consume(messageExt, false);
    }
}
