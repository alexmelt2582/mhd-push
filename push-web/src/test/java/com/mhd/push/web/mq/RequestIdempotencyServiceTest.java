package com.mhd.push.web.mq;

import com.alibaba.fastjson2.JSON;
import com.mhd.push.common.enums.ErrorCodeEnum;
import com.mhd.push.web.api.domain.SendResponse;
import com.mhd.push.web.api.service.RequestIdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RequestIdempotencyServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private RequestIdempotencyService requestIdempotencyService;

    @BeforeEach
    void setUp() {
        requestIdempotencyService = new RequestIdempotencyService();
        ReflectionTestUtils.setField(requestIdempotencyService, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(requestIdempotencyService, "enabled", true);
        ReflectionTestUtils.setField(requestIdempotencyService, "lockTtlSeconds", 10L);
        ReflectionTestUtils.setField(requestIdempotencyService, "resultTtlSeconds", 300L);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    // S11: 命中幂等缓存时直接返回历史结果，不重复执行发送逻辑
    void shouldReturnCachedResponseWhenResultExists() {
        SendResponse cached = new SendResponse("200", "ok", List.of());
        when(valueOperations.get(anyString())).thenReturn(JSON.toJSONString(cached));

        SendResponse response = requestIdempotencyService.preCheck("IDEMP-1");

        assertNotNull(response);
        assertEquals("200", response.getCode());
    }

    @Test
    // S12: 已有并发请求持锁时，应返回“处理中”
    void shouldReturnInProgressWhenLockExists() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), eq(10L), eq(TimeUnit.SECONDS))).thenReturn(Boolean.FALSE);

        SendResponse response = requestIdempotencyService.preCheck("IDEMP-2");

        assertNotNull(response);
        assertEquals(ErrorCodeEnum.REQUEST_IN_PROGRESS.getCode(), response.getCode());
    }

    @Test
    void shouldReturnNullWhenLockAcquired() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), eq(10L), eq(TimeUnit.SECONDS))).thenReturn(Boolean.TRUE);

        SendResponse response = requestIdempotencyService.preCheck("IDEMP-3");

        assertNull(response);
    }

    @Test
    // S12: 两次并发预检模拟：第一个获取锁成功，第二个同键返回处理中
    void shouldReturnInProgressForSecondConcurrentRequest() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), eq(10L), eq(TimeUnit.SECONDS)))
                .thenReturn(Boolean.TRUE)
                .thenReturn(Boolean.FALSE);

        SendResponse first = requestIdempotencyService.preCheck("IDEMP-CONCURRENT");
        SendResponse second = requestIdempotencyService.preCheck("IDEMP-CONCURRENT");

        // 首次拿到锁可继续执行业务
        assertNull(first);
        // 同键并发请求被拦截为处理中
        assertNotNull(second);
        assertEquals(ErrorCodeEnum.REQUEST_IN_PROGRESS.getCode(), second.getCode());
    }

    @Test
    // S11: 业务成功后应缓存响应并释放锁
    void shouldCacheResultOnSuccess() {
        SendResponse response = new SendResponse("200", "ok", List.of());

        requestIdempotencyService.onSuccess("IDEMP-4", response);

        verify(valueOperations, times(1)).set(anyString(), anyString(), eq(300L), eq(TimeUnit.SECONDS));
        verify(stringRedisTemplate, times(1)).delete(anyString());
    }

    @Test
    // S11/S12: 业务失败后应释放锁，避免幂等键长期阻塞
    void shouldReleaseLockOnFail() {
        requestIdempotencyService.onFail("IDEMP-5");
        verify(stringRedisTemplate, times(1)).delete(anyString());
    }
}
