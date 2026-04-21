package com.mhd.push.web.handler;

import com.mhd.push.common.domain.RecallTaskInfo;
import com.mhd.push.common.domain.TaskInfo;
import com.mhd.push.handler.flowcontrol.FlowControlFactory;
import com.mhd.push.handler.handler.BaseHandler;
import com.mhd.push.handler.handler.SendExecutionGuardService;
import com.mhd.push.handler.handler.SendGuardDecision;
import com.mhd.push.support.utils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BaseHandlerTest {

    @Mock
    private LogUtils logUtils;
    @Mock
    private FlowControlFactory flowControlFactory;
    @Mock
    private SendExecutionGuardService sendExecutionGuardService;
    @Mock
    private StringRedisTemplate stringRedisTemplate;

    private TestHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TestHandler();
        ReflectionTestUtils.setField(handler, "logUtils", logUtils);
        ReflectionTestUtils.setField(handler, "flowControlFactory", flowControlFactory);
        ReflectionTestUtils.setField(handler, "sendExecutionGuardService", sendExecutionGuardService);
        ReflectionTestUtils.setField(handler, "redisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(handler, "maxAttempts", 3);
        ReflectionTestUtils.setField(handler, "retryBackoffMs", 1L);
    }

    @Test
    void shouldRetryLocallyUntilSuccess() {
        TaskInfo taskInfo = buildTask();
        handler.failBeforeSuccess = 2;
        when(sendExecutionGuardService.tryStart(taskInfo, 99)).thenReturn(SendGuardDecision.NEW_SEND);

        handler.doHandler(taskInfo);

        assertEquals(3, handler.attempts);
        verify(sendExecutionGuardService, times(1)).markSuccess(taskInfo, 99);
        verify(sendExecutionGuardService, never()).markFail(taskInfo, 99);
    }

    @Test
    void shouldSkipWhenPendingConfirm() {
        TaskInfo taskInfo = buildTask();
        when(sendExecutionGuardService.tryStart(taskInfo, 99)).thenReturn(SendGuardDecision.PENDING_CONFIRM);

        handler.doHandler(taskInfo);

        assertEquals(0, handler.attempts);
        verify(sendExecutionGuardService, never()).markSuccess(taskInfo, 99);
        verify(sendExecutionGuardService, never()).markFail(taskInfo, 99);
    }

    private TaskInfo buildTask() {
        return TaskInfo.builder()
                .messageId("MID-1")
                .messageTemplateId(1001L)
                .sendAccount(1)
                .receiver(Set.of("u1"))
                .build();
    }

    static class TestHandler extends BaseHandler {
        private int attempts;
        private int failBeforeSuccess;

        TestHandler() {
            this.channelCode = 99;
        }

        @Override
        public boolean handler(TaskInfo taskInfo) {
            attempts++;
            return attempts > failBeforeSuccess;
        }

        @Override
        public void recall(RecallTaskInfo recallTaskInfo) {
        }
    }
}