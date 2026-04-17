package com.mhd.push.support.mq.eventbus;

import com.alibaba.fastjson2.JSON;
import com.google.common.eventbus.EventBus;
import com.mhd.push.common.domain.RecallTaskInfo;
import com.mhd.push.common.domain.TaskInfo;
import com.mhd.push.support.constants.MessageQueuePipeline;
import com.mhd.push.support.mq.SendMqService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @author zhao-hao-dong
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "mhd.mq.pipeline", havingValue = MessageQueuePipeline.EVENT_BUS)
public class EventBusSendMqServiceImpl implements SendMqService {
    private final EventBus eventBus = new EventBus();
    @Resource
    private EventBusListener eventBusListener;
    @Value("${mhd.mq.topic.send}")
    private String sendTopic;
    @Value("${mhd.mq.topic.recall}")
    private String recallTopic;

    @Override
    public void send(String topic, String jsonValue, String tagId) {
        eventBus.register(eventBusListener);
        if (topic.equals(sendTopic)) {
            eventBus.post(JSON.parseArray(jsonValue, TaskInfo.class));
        } else if (topic.equals(recallTopic)) {
            eventBus.post(JSON.parseObject(jsonValue, RecallTaskInfo.class));
        }
    }

    @Override
    public void send(String topic, String jsonValue) {
        send(topic, jsonValue, null);
    }
}
