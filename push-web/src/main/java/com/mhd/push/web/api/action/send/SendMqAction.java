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
import com.mhd.push.support.mq.SendMqService;
import com.mhd.push.web.api.domain.SendTaskModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    @Value("${mhd.mq.topic.send-orderly}")
    private String orderlySendMessageTopic;
    @Value("${mhd.mq.tagId.value}")
    private String tagId;
    @Value("${mhd.mq.pipeline}")
    private String mqPipeline;
    @Value("${mhd.mq.rocketmq.orderly.enabled:false}")
    private boolean orderlyEnabled;
    @Value("${mhd.mq.rocketmq.orderly.business-owners:}")
    private String orderlyBusinessOwners;
    @Value("${mhd.mq.retry.send.max-attempts:3}")
    private int maxSendAttempts;
    @Value("${mhd.mq.retry.send.backoff-ms:200}")
    private long sendRetryBackoffMs;


    @Override
    public void process(ProcessContext<SendTaskModel> context) {
        SendTaskModel sendTaskModel = context.getProcessModel();
        List<TaskInfo> taskInfoList = sendTaskModel.getTaskInfo();
        try {
            String message = JSON.toJSONString(taskInfoList, JSONWriter.Feature.WriteClassName);
            TaskInfo firstTaskInfo = CollUtil.getFirst(taskInfoList.listIterator());
            boolean useOrderly = shouldUseOrderly(firstTaskInfo);
            String orderKey = useOrderly ? buildOrderKey(firstTaskInfo) : null;
            String topic = useOrderly ? orderlySendMessageTopic : sendMessageTopic;
            int attempts = Math.max(maxSendAttempts, 1);
            for (int i = 1; i <= attempts; i++) {
                try {
                    sendMqService.send(topic, message, tagId, orderKey);
                    return;
                } catch (Exception ex) {
                    if (i >= attempts) {
                        throw ex;
                    }
                    log.warn("send mq retry {}/{} fail, traceId:{}, e:{}", i, attempts,
                            firstTaskInfo == null ? null : firstTaskInfo.getTraceId(), ExceptionUtil.stacktraceToString(ex));
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
                    , JSON.toJSONString(CollUtil.getFirst(taskInfoList.listIterator())));
        }
    }

    private boolean shouldUseOrderly(TaskInfo taskInfo) {
        if (!orderlyEnabled || taskInfo == null) {
            return false;
        }
        String owner = taskInfo.getBusinessOwner();
        if (owner == null || owner.isBlank()) {
            return false;
        }
        Set<String> ownerSet = parseBusinessOwners(orderlyBusinessOwners);
        return ownerSet.contains(owner);
    }

    private String buildOrderKey(TaskInfo taskInfo) {
        if (taskInfo == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (taskInfo.getBusinessOwner() != null && !taskInfo.getBusinessOwner().isBlank()
                && taskInfo.getOrderingKey() != null && !taskInfo.getOrderingKey().isBlank()) {
            sb.append(taskInfo.getBusinessOwner())
                    .append(":")
                    .append(taskInfo.getOrderingKey());
        }
        return sb.toString();
    }

    private Set<String> parseBusinessOwners(String config) {
        if (config == null || config.isBlank()) {
            return new HashSet<>();
        }
        Set<String> owners = new HashSet<>();
        Arrays.stream(config.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(owners::add);
        return owners;
    }
}
