package com.mhd.push.handler.receiver.rocketmq;

import com.alibaba.fastjson2.JSON;
import com.mhd.push.common.domain.RecallTaskInfo;
import com.mhd.push.handler.receiver.MessageReceiver;
import com.mhd.push.handler.receiver.service.ConsumeService;
import com.mhd.push.support.constants.MessageQueuePipeline;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author zhao-hao-dong
 */
@Component
@ConditionalOnProperty(name = "mhd.mq.pipeline", havingValue = MessageQueuePipeline.ROCKET_MQ)
@RocketMQMessageListener(topic = "${mhd.mq.topic.recall}",
        consumerGroup = "${mhd.mq.rocketmq.consumer.group.recall}",
        selectorType = SelectorType.TAG,
        selectorExpression = "${mhd.mq.tagId.value}"
)
public class RocketMqRecallReceiver implements RocketMQListener<String>, MessageReceiver {
    @Resource
    private ConsumeService consumeService;

    @Override
    public void onMessage(String message) {
        if (StringUtils.isBlank(message)) {
            return;
        }
        RecallTaskInfo recallTaskInfo = JSON.parseObject(message, RecallTaskInfo.class);
        consumeService.consume2recall(recallTaskInfo);
    }
}
