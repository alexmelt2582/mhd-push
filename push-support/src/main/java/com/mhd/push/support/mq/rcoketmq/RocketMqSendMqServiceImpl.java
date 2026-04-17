package com.mhd.push.support.mq.rcoketmq;

import com.mhd.push.support.constants.MessageQueuePipeline;
import com.mhd.push.support.mq.SendMqService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
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
    @Resource
    private TransactionMQProducer transactionMQProducer;

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

    ///**
    // * 内部类：事务监听器 (必须注册为 Bean)
    // * 负责协调 DB 落库 和 MQ Commit
    // */
    //@Component
    //@RocketMQTransactionListener(txProducerGroup = "pg-austin-transaction")
    //class AustinTransactionListener implements TransactionListener {
    //
    //    // A. 执行本地事务 (DB 落库)
    //    @Override
    //    public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
    //        MessageContext context = (MessageContext) arg;
    //        try {
    //            // 【关键】在 DB 插入记录，状态为 PENDING (0)
    //            recordService.savePendingRecord(context);
    //            // 返回 COMMIT，MQ 将消息对消费者可见
    //            return LocalTransactionState.COMMIT_MESSAGE;
    //        } catch (Exception e) {
    //            log.error("本地落库失败，回滚 MQ 消息", e);
    //            // 返回 ROLLBACK，MQ 丢弃该消息，保证不丢数据（因为 DB 也没写成功）
    //            return LocalTransactionState.ROLLBACK_MESSAGE;
    //        }
    //    }
    //
    //    // B. 事务回查 (防止网络抖动导致 MQ 没收到 Commit)
    //    @Override
    //    public LocalTransactionState checkLocalTransaction(MessageExt msg) {
    //        String shortCode = msg.getUserProperty("shortCode");
    //        // 检查 DB 是否存在该记录
    //        if (recordService.existsByShortCode(shortCode)) {
    //            return LocalTransactionState.COMMIT_MESSAGE;
    //        }
    //        RocketMQLocalTransactionState.
    //        return LocalTransactionState.ROLLBACK_MESSAGE;
    //    }
    //}
}
