package com.mhd.push.handler.receiver.rocketmq;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;
import com.alibaba.fastjson2.JSON;
import com.mhd.push.common.constant.RedisConstant;
import com.mhd.push.common.domain.MqDlqRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Email based ops alert for DLQ records.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "mhd.mq.dlq.alert", name = "enabled", havingValue = "true")
public class DlqMailAlertService implements DlqAlertService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${mhd.mq.dlq.alert.throttle-seconds:300}")
    private long throttleSeconds;
    @Value("${mhd.mq.dlq.alert.subject-prefix:[MHD-DLQ]}")
    private String subjectPrefix;
    @Value("${mhd.mq.dlq.alert.mail.host:}")
    private String host;
    @Value("${mhd.mq.dlq.alert.mail.port:25}")
    private Integer port;
    @Value("${mhd.mq.dlq.alert.mail.user:}")
    private String user;
    @Value("${mhd.mq.dlq.alert.mail.pass:}")
    private String pass;
    @Value("${mhd.mq.dlq.alert.mail.from:}")
    private String from;
    @Value("${mhd.mq.dlq.alert.mail.to:}")
    private String to;
    @Value("${mhd.mq.dlq.alert.mail.ssl-enable:false}")
    private boolean sslEnable;
    @Value("${mhd.mq.dlq.alert.mail.starttls-enable:false}")
    private boolean startTlsEnable;

    @Override
    public void alert(MqDlqRecord record) {
        if (record == null) {
            return;
        }
        List<String> receivers = parseReceivers(to);
        if (!isMailConfigValid(receivers)) {
            log.warn("DLQ alert skipped because mail config is incomplete, messageId:{}", record.getMessageId());
            return;
        }

        if (!tryAcquireThrottle(record)) {
            log.debug("DLQ alert throttled, messageId:{}", record.getMessageId());
            return;
        }

        MailAccount account = new MailAccount()
                .setHost(host)
                .setPort(port)
                .setAuth(true)
                .setUser(user)
                .setPass(pass)
                .setFrom(from)
                .setSslEnable(sslEnable)
                .setStarttlsEnable(startTlsEnable)
                .setTimeout(10000)
                .setConnectionTimeout(10000);

        String subject = subjectPrefix + " DLQ message requires manual compensation";
        String content = buildContent(record);
        try {
            MailUtil.send(account, receivers, subject, content, false);
            log.info("DLQ alert mail sent, messageId:{}, to:{}", record.getMessageId(), receivers);
        } catch (Exception ex) {
            log.error("DLQ alert mail send fail, messageId:{}, err:{}", record.getMessageId(), ex.getMessage(), ex);
        }
    }

    private boolean tryAcquireThrottle(MqDlqRecord record) {
        long ttl = Math.max(throttleSeconds, 1L);
        String owner = CharSequenceUtil.isBlank(record.getBusinessOwner()) ? "unknown" : record.getBusinessOwner();
        String throttleKey = RedisConstant.buildDlqAlertThrottleKey(owner);
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(throttleKey, String.valueOf(System.currentTimeMillis()), ttl, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(acquired);
    }

    private boolean isMailConfigValid(List<String> receivers) {
        return CharSequenceUtil.isNotBlank(host)
                && CharSequenceUtil.isNotBlank(user)
                && CharSequenceUtil.isNotBlank(pass)
                && CharSequenceUtil.isNotBlank(from)
                && !receivers.isEmpty();
    }

    private List<String> parseReceivers(String config) {
        if (CharSequenceUtil.isBlank(config)) {
            return List.of();
        }
        return Arrays.stream(config.split(StrPool.COMMA))
                .map(String::trim)
                .filter(CharSequenceUtil::isNotBlank)
                .toList();
    }

    private String buildContent(MqDlqRecord record) {
        return "DLQ message detected, manual handling required." + "\n"
                + "messageId=" + record.getMessageId() + "\n"
                + "bizId=" + record.getBizId() + "\n"
                + "businessOwner=" + record.getBusinessOwner() + "\n"
                + "topic=" + record.getTopic() + "\n"
                + "reconsumeTimes=" + record.getReconsumeTimes() + "/" + record.getMaxReconsumeTimes() + "\n"
                + "errorReason=" + record.getErrorReason() + "\n"
                + "status=" + record.getStatus() + "\n"
                + "recordJson=" + JSON.toJSONString(record);
    }
}
