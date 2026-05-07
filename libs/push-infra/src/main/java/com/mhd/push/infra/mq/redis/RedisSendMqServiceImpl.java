package com.mhd.push.infra.mq.redis;

import com.mhd.push.common.constant.MessageQueuePipeline;
import com.mhd.push.infra.mq.SendMqService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis 消息队列实现类
 * <p>
 * Guava Eventbus 和 Spring EventBus 只适用于单体服务
 * Redis 适合单体、微服务，且无需单独部署三方消息队列，方便开发与简单应用
 *
 * @author xiaoxiamao
 * @date 2024/7/4
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "mhd.mq.pipeline", havingValue = MessageQueuePipeline.REDIS)
public class RedisSendMqServiceImpl implements SendMqService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${mhd.mq.topic.send}")
    private String sendTopic;
    @Value("${mhd.mq.topic.recall}")
    private String recallTopic;

    /**
     * Redis 发送消息，左进右出
     *
     * @param topic
     * @param jsonValue
     * @param tagId
     */
    @Override
    public void send(String topic, String jsonValue, String tagId) {
        // 非业务topic，抛错不发送
        if (!sendTopic.equals(topic) && !recallTopic.equals(topic)) {
            log.error("RedisSendMqServiceImpl#send The topic type is not supported! topic:{}, jsonValue:{}, tagId:{}",
                    topic, jsonValue, tagId);
            return;
        }
        log.debug("RedisSendMqServiceImpl#send topic:{}, jsonValue:{}, tagId:{}", topic, jsonValue, tagId);
        stringRedisTemplate.opsForList().leftPush(topic, jsonValue);
    }

    /**
     * Redis 发送消息
     *
     * @param topic
     * @param jsonValue
     */
    @Override
    public void send(String topic, String jsonValue) {
        send(topic, jsonValue, null);
    }
}
