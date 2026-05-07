package com.mhd.push.infra.mq.rcoketmq;

import com.mhd.push.common.constant.MessageQueuePipeline;
import com.mhd.push.infra.mq.SendMqService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * @author zhao-hao-dong
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "mhd.mq.pipeline", havingValue = MessageQueuePipeline.ROCKET_MQ)
public class RocketMqSendMqServiceImpl implements SendMqService {
    @Resource
    private RocketMQTemplate rocketMqTemplate;

    @Override
    public void send(String topic, String jsonValue, String tagId) {
        if (StringUtils.isNotBlank(tagId)) {
            topic = topic + ":" + tagId;
        }
        send(topic, jsonValue);
    }

    @Override
    public void send(String topic, String jsonValue, String tagId, String orderKey) {
        if (StringUtils.isNotBlank(tagId)) {
            topic = topic + ":" + tagId;
        }
        if (StringUtils.isNotBlank(orderKey)) {
            rocketMqTemplate.syncSendOrderly(topic, MessageBuilder.withPayload(jsonValue).build(), orderKey);
            log.debug("RocketMqSendMqServiceImpl#send orderly topic:{}, orderKey:{}, jsonValue:{}", topic, orderKey, jsonValue);
            return;
        }
        send(topic, jsonValue);
    }

    @Override
    public void send(String topic, String jsonValue) {
        rocketMqTemplate.send(topic, MessageBuilder.withPayload(jsonValue).build());
        log.debug("RocketMqSendMqServiceImpl#send topic:{}, jsonValue:{}", topic, jsonValue);
    }
}
