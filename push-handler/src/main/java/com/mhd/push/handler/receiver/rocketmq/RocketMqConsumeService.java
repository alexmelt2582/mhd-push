package com.mhd.push.handler.receiver.rocketmq;

import com.alibaba.fastjson2.JSON;
import com.mhd.push.common.constant.RedisConstant;
import com.mhd.push.common.domain.MqDlqRecord;
import com.mhd.push.common.domain.TaskInfo;
import com.mhd.push.handler.receiver.service.ConsumeService;
import com.mhd.push.support.utils.LogUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * RocketMQ 消费公共逻辑：消费状态、重试失败落DLQ记录、内部告警
 */
@Slf4j
@Service
public class RocketMqConsumeService {

    @Resource
    private ConsumeService consumeService;
    @Resource
    private LogUtils logUtils;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${mhd.mq.retry.consume.max-reconsume-times:3}")
    private int maxReconsumeTimes;
    @Value("${mhd.mq.dlq.record-ttl-hours:168}")
    private long dlqRecordTtlHours;
    @Value("${mhd.mq.topic.send}")
    private String sendTopic;
    @Value("${mhd.mq.topic.send-orderly}")
    private String sendOrderlyTopic;
    @Autowired(required = false)
    private DlqAlertService dlqAlertService;

    public void consume(MessageExt messageExt, boolean orderly) {
        String message = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        if (StringUtils.isBlank(message)) {
            return;
        }
        List<TaskInfo> taskInfoLists = JSON.parseArray(message, TaskInfo.class);
        if (taskInfoLists == null || taskInfoLists.isEmpty()) {
            return;
        }
        log.debug("RocketMqBizReceiver#onMessage topic:{}, size:{}", orderly ? sendOrderlyTopic : sendTopic, taskInfoLists.size());
        try {
            if (orderly) {
                consumeService.consume2SendOrderly(taskInfoLists);
            } else {
                consumeService.consume2Send(taskInfoLists);
            }
        } catch (Exception e) {
            int currentRetry = messageExt.getReconsumeTimes() + 1;
            if (currentRetry >= maxReconsumeTimes) {
                MqDlqRecord record = recordDlq(taskInfoLists, messageExt, message, e, orderly, currentRetry);
                alertOps(record);
                log.error("RocketMqBizReceiver#onMessage fail, topic:{}, e:{}", orderly ? sendOrderlyTopic : sendTopic, e.getMessage(), e);
            }
            log.error("RocketMqBizReceiver#onMessage dispatch fail, topic:{}, e:{}", orderly ? sendOrderlyTopic : sendTopic, e.getMessage(), e);
        }
    }

    private MqDlqRecord recordDlq(List<TaskInfo> taskInfoLists, MessageExt messageExt, String payload,
                                  Exception e, boolean orderly, int currentRetry) {
        TaskInfo first = taskInfoLists.get(0);
        MqDlqRecord record = MqDlqRecord.builder()
                .messageId(first.getMessageId())
                .bizId(first.getBizId())
                .businessOwner(first.getBusinessOwner())
                .topic(orderly ? sendOrderlyTopic : sendTopic)
                .tagId(messageExt.getTags())
                .orderKey(first.getOrderKey())
                .reconsumeTimes(currentRetry)
                .maxReconsumeTimes(maxReconsumeTimes)
                .payload(payload)
                .errorReason(e.getMessage())
                .status("PENDING")
                .createdAt(System.currentTimeMillis())
                .build();

        String recordKey = RedisConstant.buildDlqRecordKey(first.getMessageId());
        stringRedisTemplate.opsForValue().set(recordKey, JSON.toJSONString(record), dlqRecordTtlHours, TimeUnit.HOURS);
        stringRedisTemplate.opsForZSet().add(RedisConstant.DLQ_INDEX_KEY, first.getMessageId(), System.currentTimeMillis());
        return record;
    }

    private void alertOps(MqDlqRecord record) {
        if (record == null || dlqAlertService == null) {
            return;
        }
        try {
            dlqAlertService.alert(record);
        } catch (Exception alertEx) {
            log.error("DLQ alert fail, messageId:{}, err:{}", record.getMessageId(), alertEx.getMessage(), alertEx);
        }
    }
}
