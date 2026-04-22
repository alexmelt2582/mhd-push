package com.mhd.push.handler.handler;

import com.alibaba.fastjson2.JSON;
import com.mhd.push.common.constant.RedisConstant;
import com.mhd.push.common.domain.TaskInfo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 发送执行守卫。
 * <p>
 * 职责有两件：
 * 1. 在真正调用第三方前写入执行台账，避免同一条消息被重复下发。
 * 2. 把超时未确认的发送转成 PENDING_CONFIRM，交给运维接口人工闭环。
 */
@Slf4j
@Service
public class SendExecutionGuardService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${mhd.handler.delivery.sending-ttl-seconds:86400}")
    private long sendingTtlSeconds;
    @Value("${mhd.handler.delivery.success-ttl-seconds:604800}")
    private long successTtlSeconds;
    @Value("${mhd.handler.delivery.fail-ttl-seconds:3600}")
    private long failTtlSeconds;
    @Value("${mhd.handler.delivery.pending-confirm-ttl-seconds:604800}")
    private long pendingConfirmTtlSeconds;
    @Value("${mhd.handler.delivery.pending-confirm-after-ms:30000}")
    private long pendingConfirmAfterMs;

    /**
     * 尝试开始一次新的发送。
     * <p>
     * 返回值语义：
     * NEW_SEND: 当前节点可以继续真正调用第三方。
     * ALREADY_SUCCESS: 此消息已经在本地确认成功，直接跳过。
     * PENDING_CONFIRM: 此消息处于待确认状态，不能再次发送到第三方。
     */
    public SendGuardDecision tryStart(TaskInfo taskInfo, Integer channelCode) {
        String key = buildExecutionKey(taskInfo, channelCode);
        long now = System.currentTimeMillis();
        SendExecutionRecord initial = buildRecord(key, taskInfo, channelCode, SendExecutionStatus.SENDING, now);
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(key, JSON.toJSONString(initial), sendingTtlSeconds, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(locked)) {
            return SendGuardDecision.NEW_SEND;
        }

        SendExecutionRecord existing = readRecord(key);
        if (existing == null) {
            stringRedisTemplate.opsForValue().set(key, JSON.toJSONString(initial), sendingTtlSeconds, TimeUnit.SECONDS);
            return SendGuardDecision.NEW_SEND;
        }

        if (SendExecutionStatus.SUCCESS.getCode().equals(existing.getStatus())) {
            return SendGuardDecision.ALREADY_SUCCESS;
        }

        if (SendExecutionStatus.SENDING.getCode().equals(existing.getStatus())) {
            if (existing.getUpdatedAt() != null && now - existing.getUpdatedAt() >= pendingConfirmAfterMs) {
                persistPendingConfirm(key, existing, now);
            }
            return SendGuardDecision.PENDING_CONFIRM;
        }

        if (SendExecutionStatus.PENDING_CONFIRM.getCode().equals(existing.getStatus())) {
            return SendGuardDecision.PENDING_CONFIRM;
        }

        stringRedisTemplate.opsForValue().set(key, JSON.toJSONString(initial), sendingTtlSeconds, TimeUnit.SECONDS);
        return SendGuardDecision.NEW_SEND;
    }

    public void markSuccess(TaskInfo taskInfo, Integer channelCode) {
        updateStatus(buildExecutionKey(taskInfo, channelCode), taskInfo, channelCode, SendExecutionStatus.SUCCESS, successTtlSeconds);
    }

    public void markFail(TaskInfo taskInfo, Integer channelCode) {
        updateStatus(buildExecutionKey(taskInfo, channelCode), taskInfo, channelCode, SendExecutionStatus.FAIL, failTtlSeconds);
    }

    /**
     * 列出最近的待确认消息，默认给运维面板使用。
     */
    public List<SendExecutionRecord> listPendingConfirmRecords(int limit) {
        long safeLimit = limit <= 0 ? 20 : limit;
        Set<String> executionKeys = stringRedisTemplate.opsForZSet().reverseRange(RedisConstant.SEND_PENDING_CONFIRM_INDEX_KEY, 0, safeLimit - 1);
        return loadRecords(executionKeys);
    }

    /**
     * 根据 traceId 查询待确认记录。
     */
    public List<SendExecutionRecord> queryPendingConfirmByTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            return Collections.emptyList();
        }
        Set<String> executionKeys = stringRedisTemplate.opsForZSet().reverseRange(RedisConstant.SEND_PENDING_CONFIRM_INDEX_KEY, 0, -1);
        return loadRecords(executionKeys).stream()
                .filter(record -> traceId.equals(record.getTraceId()))
                .collect(Collectors.toList());
    }

    /**
     * 人工确认：消息其实已经成功下发，只是本地没来得及确认。
     */
    public boolean confirmSuccess(String executionKey) {
        return updateStatusByExecutionKey(executionKey, SendExecutionStatus.SUCCESS, successTtlSeconds);
    }

    /**
     * 人工确认：消息最终失败，可允许后续重新下发。
     */
    public boolean confirmFail(String executionKey) {
        return updateStatusByExecutionKey(executionKey, SendExecutionStatus.FAIL, failTtlSeconds);
    }

    private boolean updateStatusByExecutionKey(String executionKey, SendExecutionStatus status, long ttlSeconds) {
        if (executionKey == null || executionKey.isBlank()) {
            return false;
        }
        SendExecutionRecord record = readRecord(executionKey);
        if (record == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        record.setStatus(status.getCode());
        record.setUpdatedAt(now);
        stringRedisTemplate.opsForValue().set(executionKey, JSON.toJSONString(record), ttlSeconds, TimeUnit.SECONDS);
        stringRedisTemplate.opsForZSet().remove(RedisConstant.SEND_PENDING_CONFIRM_INDEX_KEY, executionKey);
        return true;
    }

    private void updateStatus(String executionKey, TaskInfo taskInfo, Integer channelCode, SendExecutionStatus status, long ttlSeconds) {
        long now = System.currentTimeMillis();
        SendExecutionRecord record = readRecord(executionKey);
        if (record == null) {
            record = buildRecord(executionKey, taskInfo, channelCode, status, now);
        } else {
            record.setStatus(status.getCode());
            record.setUpdatedAt(now);
        }
        stringRedisTemplate.opsForValue().set(executionKey, JSON.toJSONString(record), ttlSeconds, TimeUnit.SECONDS);
        stringRedisTemplate.opsForZSet().remove(RedisConstant.SEND_PENDING_CONFIRM_INDEX_KEY, executionKey);
    }

    private void persistPendingConfirm(String executionKey, SendExecutionRecord record, long now) {
        record.setStatus(SendExecutionStatus.PENDING_CONFIRM.getCode());
        record.setUpdatedAt(now);
        stringRedisTemplate.opsForValue().set(executionKey, JSON.toJSONString(record), pendingConfirmTtlSeconds, TimeUnit.SECONDS);
        stringRedisTemplate.opsForZSet().add(RedisConstant.SEND_PENDING_CONFIRM_INDEX_KEY, executionKey, now);
    }

    private SendExecutionRecord buildRecord(String executionKey, TaskInfo taskInfo, Integer channelCode, SendExecutionStatus status, long now) {
        return SendExecutionRecord.builder()
                .executionKey(executionKey)
                .status(status.getCode())
                .traceId(taskInfo.getTraceId())
                .businessOwner(taskInfo.getBusinessOwner())
                .orderingKey(taskInfo.getOrderingKey())
                .channelCode(channelCode)
                .sendAccount(taskInfo.getSendAccount())
                .receiverSummary(buildReceiverSummary(taskInfo))
                .receiverFingerprint(buildReceiverFingerprint(taskInfo))
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private SendExecutionRecord readRecord(String key) {
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return JSON.parseObject(value, SendExecutionRecord.class);
        } catch (Exception ex) {
            log.warn("parse send execution record fail, key:{}, e:{}", key, ex.getMessage());
            return null;
        }
    }

    private List<SendExecutionRecord> loadRecords(Set<String> executionKeys) {
        if (executionKeys == null || executionKeys.isEmpty()) {
            return Collections.emptyList();
        }
        List<SendExecutionRecord> records = new ArrayList<>();
        for (String executionKey : executionKeys) {
            SendExecutionRecord record = readRecord(executionKey);
            if (record == null) {
                stringRedisTemplate.opsForZSet().remove(RedisConstant.SEND_PENDING_CONFIRM_INDEX_KEY, executionKey);
                continue;
            }
            if (!Objects.equals(record.getStatus(), SendExecutionStatus.PENDING_CONFIRM.getCode())) {
                stringRedisTemplate.opsForZSet().remove(RedisConstant.SEND_PENDING_CONFIRM_INDEX_KEY, executionKey);
                continue;
            }
            records.add(record);
        }
        return records;
    }

    private String buildExecutionKey(TaskInfo taskInfo, Integer channelCode) {
        String raw = taskInfo.getTraceId() + ":" + channelCode + ":" + taskInfo.getSendAccount() + ":" + buildReceiverFingerprint(taskInfo);
        return RedisConstant.buildSendExecutionKey(DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8)));
    }

    private String buildReceiverSummary(TaskInfo taskInfo) {
        if (taskInfo.getReceiver() == null || taskInfo.getReceiver().isEmpty()) {
            return "empty";
        }
        List<String> receivers = new ArrayList<>(taskInfo.getReceiver());
        Collections.sort(receivers);
        return String.join(",", receivers);
    }

    private String buildReceiverFingerprint(TaskInfo taskInfo) {
        if (taskInfo.getReceiver() == null || taskInfo.getReceiver().isEmpty()) {
            return "empty";
        }
        List<String> receivers = new ArrayList<>(taskInfo.getReceiver());
        Collections.sort(receivers);
        return DigestUtils.md5DigestAsHex(String.join(",", receivers).getBytes(StandardCharsets.UTF_8));
    }
}