package com.mhd.push.infra.mq.springeventbus;

import com.mhd.push.common.constant.MessageQueuePipeline;
import com.mhd.push.infra.mq.SendMqService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * @author zhao-hao-dong
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "mhd.mq.pipeline", havingValue = MessageQueuePipeline.SPRING_EVENT_BUS)
public class SpringEventBusSendMqServiceImpl implements SendMqService {
    @Resource
    private ApplicationContext applicationContext;

    @Override
    public void send(String topic, String jsonValue, String tagId) {
        MsgSpringEventSource source = MsgSpringEventSource.builder().topic(topic).jsonValue(jsonValue).tagId(tagId).build();
        MsgSpringEventBusEvent austinSpringEventBusEvent = new MsgSpringEventBusEvent(this, source);
        applicationContext.publishEvent(austinSpringEventBusEvent);
    }

    @Override
    public void send(String topic, String jsonValue) {
        send(topic, jsonValue, null);
    }
}
