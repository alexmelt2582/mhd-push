package com.mhd.push.web.mq;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.mhd.push.common.domain.TaskInfo;
import com.mhd.push.common.enums.ErrorCodeEnum;
import com.mhd.push.common.pipeline.BasicResultVO;
import com.mhd.push.common.pipeline.ProcessContext;
import com.mhd.push.support.mq.SendMqService;
import com.mhd.push.web.api.action.send.SendMqAction;
import com.mhd.push.web.api.domain.SendTaskModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendMqActionTest {

    @Mock
    private SendMqService sendMqService;

    private SendMqAction sendMqAction;

    @BeforeEach
    void setUp() {
        sendMqAction = new SendMqAction();
        ReflectionTestUtils.setField(sendMqAction, "sendMqService", sendMqService);
        ReflectionTestUtils.setField(sendMqAction, "sendMessageTopic", "austinBusiness");
        ReflectionTestUtils.setField(sendMqAction, "orderlySendMessageTopic", "austinBusinessOrderly");
        ReflectionTestUtils.setField(sendMqAction, "tagId", "mhd");
        ReflectionTestUtils.setField(sendMqAction, "mqPipeline", "rocketMq");
        ReflectionTestUtils.setField(sendMqAction, "orderlyEnabled", true);
        ReflectionTestUtils.setField(sendMqAction, "orderlyBusinessOwners", "order-center,pay-center");
        ReflectionTestUtils.setField(sendMqAction, "maxSendAttempts", 3);
        ReflectionTestUtils.setField(sendMqAction, "sendRetryBackoffMs", 1L);
        ReflectionTestUtils.setField(sendMqAction, "maxPayloadSizeBytes", 1024 * 1024);
    }

    @Test
        // S1: 有序业务方A应进入有序topic，并生成稳定顺序键
    void shouldRouteToOrderlyTopicForConfiguredOwner() {
        doNothing().when(sendMqService).send(anyString(), anyString(), anyString(), any());

        TaskInfo taskInfo = buildTask("order-center", null);
        ProcessContext<SendTaskModel> context = buildContext(taskInfo);

        sendMqAction.process(context);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> orderKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendMqService, times(1)).send(topicCaptor.capture(), anyString(), anyString(), orderKeyCaptor.capture());

        assertEquals("austinBusinessOrderly", topicCaptor.getValue());
        assertEquals("order-center:ORDER-1", orderKeyCaptor.getValue());
    }

    @Test
        // S2: 有序业务方B传入自定义orderKey时，应优先使用业务指定值
    void shouldUseCustomOrderKeyWhenProvided() {
        doNothing().when(sendMqService).send(anyString(), anyString(), anyString(), any());

        TaskInfo taskInfo = buildTask("pay-center", "order-flow-001");
        ProcessContext<SendTaskModel> context = buildContext(taskInfo);

        sendMqAction.process(context);

        ArgumentCaptor<String> orderKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendMqService, times(1)).send(anyString(), anyString(), anyString(), orderKeyCaptor.capture());
        assertEquals("order-flow-001", orderKeyCaptor.getValue());
    }

    @Test
        // S3/S4: 非白名单业务方必须走普通topic，避免占用有序消费资源
    void shouldRouteToCommonTopicForUnorderedOwner() {
        doNothing().when(sendMqService).send(anyString(), anyString(), anyString(), any());

        TaskInfo taskInfo = buildTask("notice-center", null);
        ProcessContext<SendTaskModel> context = buildContext(taskInfo);

        sendMqAction.process(context);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> orderKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendMqService, times(1)).send(topicCaptor.capture(), anyString(), anyString(), orderKeyCaptor.capture());

        assertEquals("austinBusiness", topicCaptor.getValue());
        assertNull(orderKeyCaptor.getValue());
    }

    @Test
        // S5: 同业务链路跨渠道（短信/邮件）发送时，顺序键应保持一致
    void shouldKeepStableOrderKeyAcrossChannelsForSameBiz() {
        doNothing().when(sendMqService).send(anyString(), anyString(), anyString(), any());

        TaskInfo smsTask = buildTask("order-center", null);
        smsTask.setSendChannel(30);
        TaskInfo mixedTask = buildTask("order-center", null);
        mixedTask.setSendChannel(40);
        TaskInfo mailTask = buildTask("order-center", null);
        mailTask.setSendChannel(20);
        SendTaskModel model = SendTaskModel.builder().taskInfo(List.of(smsTask, mixedTask, mailTask)).build();
        ProcessContext<SendTaskModel> context = ProcessContext.<SendTaskModel>builder()
                .processModel(model)
                .needBreak(false)
                .response(BasicResultVO.success())
                .build();

        sendMqAction.process(context);

        ArgumentCaptor<String> orderKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendMqService, times(1)).send(anyString(), anyString(), anyString(), orderKeyCaptor.capture());
        // 关键断言：跨渠道时顺序键仍然只由业务链路决定
        assertEquals("order-center:ORDER-5001", orderKeyCaptor.getValue());
    }

    @Test
        // S6: 发送侧失败时应按配置重试，直到成功或耗尽次数
    void shouldRetryWhenSendFails() {
        TaskInfo taskInfo = buildTask("order-center", null);
        ProcessContext<SendTaskModel> context = buildContext(taskInfo);

        doThrow(new RuntimeException("first"))
                .doThrow(new RuntimeException("second"))
                .doNothing()
                .when(sendMqService)
                .send(anyString(), anyString(), anyString(), any());

        sendMqAction.process(context);

        verify(sendMqService, times(3)).send(anyString(), anyString(), anyString(), any());
    }

    @Test
        // S13: 消息体超限时必须在入MQ前被拒绝
    void shouldFailWhenPayloadTooLarge() {
        ReflectionTestUtils.setField(sendMqAction, "maxPayloadSizeBytes", 1);
        TaskInfo taskInfo = buildTask("order-center", null);
        ProcessContext<SendTaskModel> context = buildContext(taskInfo);

        sendMqAction.process(context);

        assertEquals(ErrorCodeEnum.MESSAGE_PAYLOAD_TOO_LARGE.getCode(), context.getResponse().getStatus());
        verify(sendMqService, times(0)).send(anyString(), anyString(), anyString(), any());
    }

    @Test
        // S14: 边界值校验，消息体大小等于阈值时允许发送
    void shouldAllowWhenPayloadSizeEqualsThreshold() {
        TaskInfo taskInfo = buildTask("order-center", null);
        String payload = JSON.toJSONString(List.of(taskInfo), JSONWriter.Feature.WriteClassName);
        ReflectionTestUtils.setField(sendMqAction, "maxPayloadSizeBytes", payload.getBytes(StandardCharsets.UTF_8).length);

        doNothing().when(sendMqService).send(anyString(), anyString(), anyString(), any());
        ProcessContext<SendTaskModel> context = buildContext(taskInfo);

        sendMqAction.process(context);

        assertEquals(ErrorCodeEnum.SUCCESS.getCode(), context.getResponse().getStatus());
        verify(sendMqService, times(1)).send(anyString(), anyString(), anyString(), any());
    }

    private TaskInfo buildTask(String businessOwner, String orderKey) {
        return TaskInfo.builder()
                .traceId("MID-" + orderKey)
                .businessOwner(businessOwner)
                .orderingKey(orderKey)
                .templateId(1001L)
                .sendChannel(20)
                .build();
    }

    private ProcessContext<SendTaskModel> buildContext(TaskInfo taskInfo) {
        SendTaskModel model = SendTaskModel.builder().taskInfo(List.of(taskInfo)).build();
        return ProcessContext.<SendTaskModel>builder()
                .processModel(model)
                .needBreak(false)
                .response(BasicResultVO.success())
                .build();
    }
}
