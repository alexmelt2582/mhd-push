package com.mhd.push.worker.receiver.rocketmq;

import com.mhd.push.common.constant.MessageQueuePipeline;
import com.mhd.push.engine.receiver.MessageReceiver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 有序消费通道（仅业务方白名单命中时才会投递到该topic）
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "mhd.mq.pipeline", havingValue = MessageQueuePipeline.ROCKET_MQ)
@RocketMQMessageListener(topic = "${mhd.mq.topic.send-orderly}",
        consumerGroup = "${mhd.mq.rocketmq.consumer.group.send-orderly}",
        selectorType = SelectorType.TAG,
        selectorExpression = "${mhd.mq.tagId.value}",
    consumeMode = ConsumeMode.ORDERLY,
    maxReconsumeTimes = 3)
public class RocketMqOrderlyBizReceiver implements RocketMQListener<MessageExt>, MessageReceiver {

    @Resource
    private RocketMqConsumeService rocketMqConsumeService;

    @Override
    public void onMessage(MessageExt messageExt) {
        rocketMqConsumeService.consume(messageExt, true);
    }
}
