package com.mhd.push.publicapi.action.send;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.mhd.push.common.enums.MsgPushState;
import com.mhd.push.common.enums.SendTypeEnum;
import com.mhd.push.common.log.LogRecord;
import com.mhd.push.common.pipeline.BasicResultVO;
import com.mhd.push.common.pipeline.BusinessProcess;
import com.mhd.push.common.pipeline.ProcessContext;
import com.mhd.push.domain.model.task.TaskInfo;
import com.mhd.push.infra.mq.SendMqService;
import com.mhd.push.infra.utils.LogUtils;
import com.mhd.push.publicapi.domain.SendTaskModel;
import com.mhd.push.publicapi.exception.ClientErrorCodeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * MQ 发送动作。
 * <p>
 * 该动作负责将已组装的发送任务投递到消息通道，并在通道不可用时返回对调用方更友好的提示信息。
 * </p>
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
    @Resource
    private LogUtils logUtils;


    @Override
    public void process(ProcessContext<SendTaskModel> context) {
        SendTaskModel sendTaskModel = context.getProcessModel();
        List<TaskInfo> taskInfoList = sendTaskModel.getTaskInfo();
        try {
            // 1. 将任务列表统一序列化为 MQ 消息体。
            String message = JSON.toJSONString(taskInfoList, JSONWriter.Feature.WriteClassName);
            TaskInfo firstTaskInfo = CollUtil.getFirst(taskInfoList.listIterator());
            boolean useOrderly = shouldUseOrderly(firstTaskInfo);
            String orderKey = useOrderly ? buildOrderKey(firstTaskInfo) : null;
            String topic = useOrderly ? orderlySendMessageTopic : sendMessageTopic;
            int attempts = Math.max(maxSendAttempts, 1);
            for (int i = 1; i <= attempts; i++) {
                try {
                    // 2. 短暂抖动优先在服务端内部重试，不直接暴露给调用方。
                    sendMqService.send(topic, message, tagId, orderKey);
                    for (TaskInfo taskInfo : taskInfoList) {
                        LogRecord logRecord = LogRecord.build(SendTypeEnum.SEND, taskInfo, MsgPushState.SEND_MQ_SUCCESS);
                        logUtils.print(logRecord);
                    }
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
            log.error("send {} fail! e:{},params:{}", mqPipeline, ExceptionUtil.stacktraceToString(e)
                    , JSON.toJSONString(CollUtil.getFirst(taskInfoList.listIterator())));
            // MQ 投递失败时写入 trace，保留内部排查细节，但不影响外部用户统一失败文案。
            for (TaskInfo taskInfo : taskInfoList) {
                LogRecord logRecord = LogRecord.build(SendTypeEnum.SEND, taskInfo, MsgPushState.SEND_MQ_FAIL);
                logUtils.print(logRecord);
            }
            context.setNeedBreak(true).setResponse(BasicResultVO.fail(ClientErrorCodeEnum.CLIENT_SEND_CHANNEL_UNAVAILABLE));
        }
    }

    /**
     * 判断当前任务是否需要走有序 Topic。
     *
     * @param taskInfo 任务信息
     * @return true 表示命中有序发送规则
     */
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

    /**
     * 构造顺序消息使用的分片键。
     *
     * @param taskInfo 任务信息
     * @return 分片键
     */
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

    /**
     * 解析配置中的业务方白名单。
     *
     * @param config 逗号分隔的业务方配置
     * @return 业务方集合
     */
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
