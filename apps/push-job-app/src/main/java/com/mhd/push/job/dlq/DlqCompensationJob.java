package com.mhd.push.job.dlq;

import com.alibaba.fastjson2.JSON;
import com.mhd.push.common.constant.RedisConstant;
import com.mhd.push.domain.model.mq.MqDlqRecord;
import com.mhd.push.infra.mq.SendMqService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * DLQ 补偿任务。
 */
@Slf4j
@Component
public class DlqCompensationJob {
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_COMPENSATED = "COMPENSATED";
    private static final String STATUS_RETRY_FAIL = "RETRY_FAIL";

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private SendMqService sendMqService;

    @Value("${mhd.mq.dlq.record-ttl-hours:168}")
    private long dlqRecordTtlHours;
    @Value("${mhd.mq.dlq.compensation.batch-size:20}")
    private int defaultBatchSize;

    /**
     * 通过 traceId 手动补偿单条 DLQ 记录。
     * jobParam: traceId
     */
    @XxlJob("dlqReplayByTraceIdJob")
    public void replayByTraceId() {
        String traceId = trimToNull(XxlJobHelper.getJobParam());
        if (traceId == null) {
            XxlJobHelper.handleFail("jobParam must be traceId");
            return;
        }
        CompensationResult result = compensate(traceId);
        if (!result.success()) {
            XxlJobHelper.handleFail(result.message());
            return;
        }
        XxlJobHelper.handleSuccess(result.message());
    }

    /**
     * 批量扫描最早进入 DLQ 的记录并尝试补偿。
     * jobParam: batch size，可选
     */
    @XxlJob("dlqReplayBatchJob")
    public void replayBatch() {
        int batchSize = parseBatchSize(XxlJobHelper.getJobParam());
        Set<String> traceIds = stringRedisTemplate.opsForZSet().range(RedisConstant.DLQ_INDEX_KEY, 0, batchSize - 1L);
        if (traceIds == null || traceIds.isEmpty()) {
            XxlJobHelper.handleSuccess("no pending dlq records");
            return;
        }

        int successCount = 0;
        int skipCount = 0;
        int failCount = 0;
        for (String traceId : traceIds) {
            CompensationResult result = compensate(traceId);
            if (result.success()) {
                successCount++;
                continue;
            }
            if (result.skipped()) {
                skipCount++;
                continue;
            }
            failCount++;
        }
        String summary = "dlq batch replay finished, total=" + traceIds.size()
                + ", success=" + successCount
                + ", skipped=" + skipCount
                + ", failed=" + failCount;
        if (failCount > 0) {
            XxlJobHelper.handleFail(summary);
            return;
        }
        XxlJobHelper.handleSuccess(summary);
    }

    private CompensationResult compensate(String traceId) {
        String recordKey = RedisConstant.buildDlqRecordKey(traceId);
        String json = stringRedisTemplate.opsForValue().get(recordKey);
        if (json == null || json.isBlank()) {
            return new CompensationResult(false, true, "dlq record not found, traceId=" + traceId);
        }

        MqDlqRecord record = JSON.parseObject(json, MqDlqRecord.class);
        if (record == null) {
            return new CompensationResult(false, false, "dlq record parse fail, traceId=" + traceId);
        }
        if (STATUS_COMPENSATED.equalsIgnoreCase(record.getStatus())) {
            stringRedisTemplate.opsForZSet().remove(RedisConstant.DLQ_INDEX_KEY, traceId);
            return new CompensationResult(false, true, "dlq record already compensated, traceId=" + traceId);
        }
        if (record.getTopic() == null || record.getTopic().isBlank() || record.getPayload() == null || record.getPayload().isBlank()) {
            return new CompensationResult(false, false, "dlq record missing topic or payload, traceId=" + traceId);
        }

        try {
            sendMqService.send(record.getTopic(), record.getPayload(), record.getTagId(), record.getOrderKey());
            record.setStatus(STATUS_COMPENSATED);
            record.setCompensatedAt(System.currentTimeMillis());
            stringRedisTemplate.opsForValue().set(recordKey, JSON.toJSONString(record), Math.max(dlqRecordTtlHours, 1L), TimeUnit.HOURS);
            stringRedisTemplate.opsForZSet().remove(RedisConstant.DLQ_INDEX_KEY, traceId);
            log.info("dlq compensate success, traceId:{}, topic:{}", traceId, record.getTopic());
            return new CompensationResult(true, false, "dlq compensate success, traceId=" + traceId);
        } catch (Exception ex) {
            record.setStatus(STATUS_RETRY_FAIL);
            record.setErrorReason(ex.getMessage());
            stringRedisTemplate.opsForValue().set(recordKey, JSON.toJSONString(record), Math.max(dlqRecordTtlHours, 1L), TimeUnit.HOURS);
            log.error("dlq compensate fail, traceId:{}, err:{}", traceId, ex.getMessage(), ex);
            return new CompensationResult(false, false, "dlq compensate fail, traceId=" + traceId + ", err=" + ex.getMessage());
        }
    }

    private int parseBatchSize(String jobParam) {
        String value = trimToNull(jobParam);
        if (value == null) {
            return normalizeBatchSize(defaultBatchSize);
        }
        try {
            return normalizeBatchSize(Integer.parseInt(value));
        } catch (NumberFormatException ex) {
            return normalizeBatchSize(defaultBatchSize);
        }
    }

    private int normalizeBatchSize(int batchSize) {
        if (batchSize <= 0) {
            return 20;
        }
        return Math.min(batchSize, 200);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record CompensationResult(boolean success, boolean skipped, String message) {
    }
}