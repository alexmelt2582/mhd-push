package com.mhd.push.web.service;

import com.alibaba.fastjson2.JSON;
import com.mhd.push.common.constant.MqRedisConstant;
import com.mhd.push.common.domain.MqDlqRecord;
import com.mhd.push.common.enums.ErrorCodeEnum;
import com.mhd.push.support.mq.SendMqService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * DLQ回访与人工补偿服务
 */
@Service
public class DlqCompensationService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private SendMqService sendMqService;

    public List<MqDlqRecord> list(int pageNo, int pageSize) {
        int safePageNo = Math.max(pageNo, 1);
        int safePageSize = Math.max(pageSize, 1);
        long start = (long) (safePageNo - 1) * safePageSize;
        long end = start + safePageSize - 1;
        Set<String> messageIds = stringRedisTemplate.opsForZSet().reverseRange(MqRedisConstant.DLQ_INDEX_KEY, start, end);
        List<MqDlqRecord> result = new ArrayList<>();
        if (messageIds == null || messageIds.isEmpty()) {
            return result;
        }
        for (String messageId : messageIds) {
            MqDlqRecord record = getByMessageId(messageId);
            if (record != null) {
                result.add(record);
            }
        }
        return result;
    }

    public MqDlqRecord getByMessageId(String messageId) {
        String raw = stringRedisTemplate.opsForValue().get(MqRedisConstant.DLQ_RECORD_KEY_PREFIX + messageId);
        if (raw == null) {
            return null;
        }
        return JSON.parseObject(raw, MqDlqRecord.class);
    }

    public ErrorCodeEnum compensate(String messageId) {
        MqDlqRecord record = getByMessageId(messageId);
        if (record == null) {
            return ErrorCodeEnum.DLQ_RECORD_NOT_FOUND;
        }
        sendMqService.send(record.getTopic(), record.getPayload(), record.getTagId(), record.getOrderKey());

        record.setStatus("COMPENSATED");
        record.setCompensatedAt(System.currentTimeMillis());
        stringRedisTemplate.opsForValue().set(MqRedisConstant.DLQ_RECORD_KEY_PREFIX + messageId, JSON.toJSONString(record));
        return ErrorCodeEnum.SUCCESS;
    }
}
