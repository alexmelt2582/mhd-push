package com.mhd.push.web.api.action.send;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.mhd.push.common.domain.TaskInfo;
import com.mhd.push.common.enums.ErrorCodeEnum;
import com.mhd.push.common.pipeline.BasicResultVO;
import com.mhd.push.common.pipeline.BusinessProcess;
import com.mhd.push.common.pipeline.ProcessContext;
import com.mhd.push.web.api.domain.SendTaskModel;
import com.mhd.push.support.mq.SendMqService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 1. 将消息发送到MQ
 * 2. 返回拼装好的messageId给到接口调用方
 *
 * @author zhao-hao-dong

 */
@Slf4j
@Service
public class SendMqAction implements BusinessProcess<SendTaskModel> {
    @Resource
    private SendMqService sendMqService;
    @Value("${mhd.mq.topic.send}")
    private String sendMessageTopic;
    @Value("${mhd.mq.tagId.value}")
    private String tagId;
    @Value("${mhd.mq.pipeline}")
    private String mqPipeline;
    @Value("${mhd.mq.rocketmq.orderly.enabled:false}")
    private boolean orderlyEnabled;
    @Value("${mhd.mq.rocketmq.orderly.key-mode:bizId}")
    private String orderlyKeyMode;
    @Value("${mhd.mq.retry.send.max-attempts:3}")
    private int maxSendAttempts;
    @Value("${mhd.mq.retry.send.backoff-ms:200}")
    private long sendRetryBackoffMs;
    @Value("${mhd.mq.payload.max-size-bytes:3145728}")
    private int maxPayloadSizeBytes;

    @Override
    public void process(ProcessContext<SendTaskModel> context) {
        SendTaskModel sendTaskModel = context.getProcessModel();
        List<TaskInfo> taskInfo = sendTaskModel.getTaskInfo();
        try{
            String message = JSON.toJSONString(taskInfo, JSONWriter.Feature.WriteClassName);
            // 消息内容是否超出限制大小
            if (message.getBytes(StandardCharsets.UTF_8).length > maxPayloadSizeBytes) {
                context.setNeedBreak(true).setResponse(BasicResultVO.fail(ErrorCodeEnum.MESSAGE_PAYLOAD_TOO_LARGE));
                return;
            }

            TaskInfo firstTaskInfo = CollUtil.getFirst(taskInfo.listIterator());
            String orderKey = buildOrderKey(firstTaskInfo);
            int attempts = Math.max(maxSendAttempts, 1);
            for (int i = 1; i <= attempts; i++) {
                try {
                    sendMqService.send(sendMessageTopic, message, tagId, orderKey);
                    return;
                } catch (Exception ex) {
                    if (i >= attempts) {
                        throw ex;
                    }
                    log.warn("send mq retry {}/{} fail, messageId:{}, e:{}", i, attempts,
                            firstTaskInfo == null ? null : firstTaskInfo.getMessageId(), ExceptionUtil.stacktraceToString(ex));
                    try {
                        Thread.sleep(sendRetryBackoffMs);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        throw ex;
                    }
                }
            }
        } catch (Exception e) {
            context.setNeedBreak(true).setResponse(BasicResultVO.fail(ErrorCodeEnum.SERVICE_ERROR));
            log.error("send {} fail! e:{},params:{}", mqPipeline, ExceptionUtil.stacktraceToString(e)
                    , JSON.toJSONString(CollUtil.getFirst(taskInfo.listIterator())));
        }
    }

    private String buildOrderKey(TaskInfo taskInfo) {
        if (!orderlyEnabled || taskInfo == null) {
            return null;
        }
        if ("template".equalsIgnoreCase(orderlyKeyMode)) {
            return String.valueOf(taskInfo.getMessageTemplateId());
        }
        if ("channel".equalsIgnoreCase(orderlyKeyMode)) {
            return String.valueOf(taskInfo.getSendChannel());
        }
        if (taskInfo.getBizId() != null && !taskInfo.getBizId().isBlank()) {
            return taskInfo.getBizId();
        }
        return taskInfo.getMessageId();
    }
}
