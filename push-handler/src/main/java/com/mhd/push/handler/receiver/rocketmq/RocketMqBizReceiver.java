package com.mhd.push.handler.receiver.rocketmq;

import com.alibaba.fastjson2.JSON;
import com.mhd.push.common.domain.MsgPushLogRequest;
import com.mhd.push.common.domain.TaskInfo;
import com.mhd.push.common.enums.MsgPushState;
import com.mhd.push.common.enums.MsgPushTypeEnum;
import com.mhd.push.handler.receiver.MessageReceiver;
import com.mhd.push.handler.receiver.service.ConsumeService;
import com.mhd.push.support.constants.MessageQueuePipeline;
import com.mhd.push.support.utils.LogUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author zhao-hao-dong
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "mhd.mq.pipeline", havingValue = MessageQueuePipeline.ROCKET_MQ)
@RocketMQMessageListener(topic = "${mhd.mq.topic.send}",
        consumerGroup = "${mhd.mq.rocketmq.consumer.group.send}",
        selectorType = SelectorType.TAG,
        selectorExpression = "${mhd.mq.tagId.value}",
        consumeMode = ConsumeMode.ORDERLY,
        maxReconsumeTimes = 3
)
public class RocketMqBizReceiver implements RocketMQListener<String>, MessageReceiver {
    @Resource
    private ConsumeService consumeService;
    @Resource
    private LogUtils logUtils;
    @Value("${mhd.mq.topic.send}")
    private String topic;

    @Override
    public void onMessage(String message) {
        if (StringUtils.isBlank(message)) {
            return;
        }
        List<TaskInfo> taskInfoLists = JSON.parseArray(message, TaskInfo.class);
        log.debug("RocketMqBizReceiver#onMessage topic:{}, size:{}", topic, taskInfoLists.size());
        try {
            consumeService.consume2Send(taskInfoLists);
        } catch (Exception e) {
            log.error("RocketMqBizReceiver#onMessage fail, topic:{}, e:{}", topic, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
