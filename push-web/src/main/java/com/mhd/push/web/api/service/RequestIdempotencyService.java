package com.mhd.push.web.api.service;

import com.alibaba.fastjson2.JSON;
import com.mhd.push.common.constant.MqRedisConstant;
import com.mhd.push.common.enums.ErrorCodeEnum;
import com.mhd.push.web.api.domain.SendResponse;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 接口级幂等处理服务
 */
@Service
public class RequestIdempotencyService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${mhd.idempotency.enabled:true}")
    private boolean enabled;
    @Value("${mhd.idempotency.lock-ttl-seconds:10}")
    private long lockTtlSeconds;
    @Value("${mhd.idempotency.result-ttl-seconds:300}")
    private long resultTtlSeconds;

    public String buildRequestFingerprint(String apiName, Object payload) {
        String raw = apiName + ":" + JSON.toJSONString(payload);
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    public SendResponse preCheck(String idempotencyKey) {
        if (!enabled) {
            return null;
        }
        String result = stringRedisTemplate.opsForValue().get(getResultKey(idempotencyKey));
        if (result != null) {
            return JSON.parseObject(result, SendResponse.class);
        }

        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(getLockKey(idempotencyKey), "1", lockTtlSeconds, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            return new SendResponse(ErrorCodeEnum.REQUEST_IN_PROGRESS.getCode(), ErrorCodeEnum.REQUEST_IN_PROGRESS.getMessage(), null);
        }
        return null;
    }

    public void onSuccess(String idempotencyKey, SendResponse response) {
        if (!enabled) {
            return;
        }
        stringRedisTemplate.opsForValue().set(getResultKey(idempotencyKey), JSON.toJSONString(response), resultTtlSeconds, TimeUnit.SECONDS);
        stringRedisTemplate.delete(getLockKey(idempotencyKey));
    }

    public void onFail(String idempotencyKey) {
        if (!enabled) {
            return;
        }
        stringRedisTemplate.delete(getLockKey(idempotencyKey));
    }

    private String getResultKey(String idempotencyKey) {
        return MqRedisConstant.IDEMPOTENCY_RESULT_KEY_PREFIX + idempotencyKey;
    }

    private String getLockKey(String idempotencyKey) {
        return MqRedisConstant.IDEMPOTENCY_LOCK_KEY_PREFIX + idempotencyKey;
    }
}
