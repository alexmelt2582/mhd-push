package com.mhd.push.handler.receiver.springeventbus;

import com.alibaba.fastjson2.JSON;
import com.mhd.push.common.domain.RecallTaskInfo;
import com.mhd.push.common.domain.TaskInfo;
import com.mhd.push.handler.receiver.MessageReceiver;
import com.mhd.push.support.constants.MessageQueuePipeline;
import com.mhd.push.support.mq.springeventbus.MsgSpringEventBusEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

/**
 * @author zhao-hao-dong
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "mhd.mq.pipeline", havingValue = MessageQueuePipeline.SPRING_EVENT_BUS)

public class SpringEventBusReceiverListener implements ApplicationListener<MsgSpringEventBusEvent>, MessageReceiver {
    @Resource
    private SpringEventBusReceiver springEventBusReceiver;
    @Value("${mhd.mq.topic.send}")
    private String sendTopic;
    @Value("${mhd.mq.topic.recall}")
    private String recallTopic;

    @Override
    public void onApplicationEvent(MsgSpringEventBusEvent event) {
        String topic = event.getMsgSpringEventSource().getTopic();
        String jsonValue = event.getMsgSpringEventSource().getJsonValue();
        if (topic.equals(sendTopic)) {
            springEventBusReceiver.consume(JSON.parseArray(jsonValue, TaskInfo.class));
        } else if (topic.equals(recallTopic)) {
            springEventBusReceiver.recall(JSON.parseObject(jsonValue, RecallTaskInfo.class));
        }
    }
}
