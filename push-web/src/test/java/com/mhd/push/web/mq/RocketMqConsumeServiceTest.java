package com.mhd.push.web.mq;

import com.alibaba.fastjson2.JSON;
import com.mhd.push.common.constant.RedisConstant;
import com.mhd.push.common.domain.TaskInfo;
import com.mhd.push.handler.receiver.rocketmq.DlqAlertService;
import com.mhd.push.handler.receiver.rocketmq.RocketMqConsumeService;
import com.mhd.push.handler.receiver.service.ConsumeService;
import com.mhd.push.support.utils.LogUtils;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RocketMqConsumeServiceTest {

    @Mock
    private ConsumeService consumeService;
    @Mock
    private LogUtils logUtils;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private ZSetOperations<String, String> zSetOperations;
    @Mock
    private DlqAlertService dlqAlertService;

    private RocketMqConsumeService rocketMqConsumeService;

    @BeforeEach
    void setUp() {
        rocketMqConsumeService = new RocketMqConsumeService();
        ReflectionTestUtils.setField(rocketMqConsumeService, "consumeService", consumeService);
        ReflectionTestUtils.setField(rocketMqConsumeService, "logUtils", logUtils);
        ReflectionTestUtils.setField(rocketMqConsumeService, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(rocketMqConsumeService, "maxReconsumeTimes", 3);
        ReflectionTestUtils.setField(rocketMqConsumeService, "dlqRecordTtlHours", 168L);
        ReflectionTestUtils.setField(rocketMqConsumeService, "sendTopic", "austinBusiness");
        ReflectionTestUtils.setField(rocketMqConsumeService, "sendOrderlyTopic", "austinBusinessOrderly");
        ReflectionTestUtils.setField(rocketMqConsumeService, "dlqAlertService", dlqAlertService);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    // S8: 消费重试次数达到阈值后应写入DLQ记录
    void shouldRecordDlqWhenRetryExhausted() {
        MessageExt messageExt = buildMessage(2, "mhd", buildTask("order-center", "ORDER-1"));
        doThrow(new RuntimeException("consume-fail")).when(consumeService).consume2SendOrderly(any());

        rocketMqConsumeService.consume(messageExt, true);

        verify(valueOperations, times(1)).set(anyString(), anyString(), eq(168L), eq(TimeUnit.HOURS));
        verify(zSetOperations, times(1)).add(eq(RedisConstant.DLQ_INDEX_KEY), eq("MID-ORDER-1"), anyDouble());
        verify(dlqAlertService, times(1)).alert(any());
    }

    @Test
    // S8反向用例：未达到阈值时不应写入DLQ记录
    void shouldNotRecordDlqBeforeRetryThreshold() {
        MessageExt messageExt = buildMessage(0, "mhd", buildTask("order-center", "ORDER-2"));
        doThrow(new RuntimeException("consume-fail")).when(consumeService).consume2Send(any());

        rocketMqConsumeService.consume(messageExt, false);

        verify(valueOperations, times(0)).set(anyString(), anyString(), anyLong(), any());
        verify(zSetOperations, times(0)).add(anyString(), anyString(), anyDouble());
        verify(dlqAlertService, times(0)).alert(any());
    }

    @Test
    // S7: 普通消息应进入普通消费路径
    void shouldDispatchNormalMessagesToAsyncPath() {
        MessageExt messageExt = buildMessage(0, "mhd", buildTask("notice-center", "NOTICE-1"));
        doNothing().when(consumeService).consume2Send(any());

        rocketMqConsumeService.consume(messageExt, false);

        verify(consumeService, times(1)).consume2Send(any());
        verify(consumeService, never()).consume2SendOrderly(any());
        verify(valueOperations, times(0)).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    // S7: 顺序消息应进入按顺序键分流的消费路径
    void shouldDispatchOrderlyMessagesToOrderlyPath() {
        MessageExt messageExt = buildMessage(0, "mhd", buildTask("order-center", "ORDER-RETRY"));
        doNothing().when(consumeService).consume2SendOrderly(any());

        rocketMqConsumeService.consume(messageExt, true);

        verify(consumeService, times(1)).consume2SendOrderly(any());
        verify(consumeService, never()).consume2Send(any());
        verify(valueOperations, times(0)).set(anyString(), anyString(), eq(168L), eq(TimeUnit.HOURS));
    }

    @Test
    // S10: 最终失败后应触发内部告警，交由运维/值班人员手工处理
    void shouldAlertOpsWhenRetryExhausted() {
        ReflectionTestUtils.setField(rocketMqConsumeService, "maxReconsumeTimes", 1);
        TaskInfo taskInfo = buildTask("order-center", "ORDER-ALERT");
        MessageExt messageExt = buildMessage(0, "mhd", taskInfo);
        doThrow(new RuntimeException("alert-fail")).when(consumeService).consume2SendOrderly(any());

        rocketMqConsumeService.consume(messageExt, true);

        verify(dlqAlertService, times(1)).alert(any());
    }

    private MessageExt buildMessage(int reconsumeTimes, String tag, TaskInfo taskInfo) {
        MessageExt messageExt = new MessageExt();
        messageExt.setBody(JSON.toJSONString(List.of(taskInfo)).getBytes(StandardCharsets.UTF_8));
        messageExt.setReconsumeTimes(reconsumeTimes);
        messageExt.setTags(tag);
        return messageExt;
    }

    private TaskInfo buildTask(String owner, String bizId) {
        return TaskInfo.builder()
                .messageId("MID-" + bizId)
                .bizId(bizId)
                .businessOwner(owner)
                .messageTemplateId(1002L)
                .build();
    }
}
