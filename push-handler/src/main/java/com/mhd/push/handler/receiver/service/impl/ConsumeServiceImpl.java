package com.mhd.push.handler.receiver.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.mhd.push.common.domain.*;
import com.mhd.push.common.enums.MsgPushState;
import com.mhd.push.common.enums.MsgPushTypeEnum;
import com.mhd.push.handler.handler.HandlerHolder;
import com.mhd.push.handler.pending.Task;
import com.mhd.push.handler.pending.TaskPendingHolder;
import com.mhd.push.handler.receiver.service.ConsumeService;
import com.mhd.push.handler.utils.GroupIdMappingUtils;
import com.mhd.push.support.utils.LogUtils;
import jakarta.annotation.Resource;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author zhao-hao-dong
 */
@Service
public class ConsumeServiceImpl implements ConsumeService {
    private static final String LOG_BIZ_TYPE = "Receiver#consumer";
    private static final String LOG_BIZ_RECALL_TYPE = "Receiver#recall";

    @Resource
    private TaskPendingHolder taskPendingHolder;
    @Resource
    private ApplicationContext context;
    @Resource
    private LogUtils logUtils;
    @Resource
    private HandlerHolder handlerHolder;

    /**
     * 消费 MQ 消息后，将任务提交到分发线程池。
     */
    @Override
    public void consume2Send(List<TaskInfo> taskInfoLists) {
        String topicGroupId = GroupIdMappingUtils.getGroupIdByTaskInfo(CollUtil.getFirst(taskInfoLists.iterator()));
        long currentTimeMillis = System.currentTimeMillis();
        for (TaskInfo taskInfo : taskInfoLists) {
            logReceive(taskInfo, currentTimeMillis);
            Task task = context.getBean(Task.class).setTaskInfo(taskInfo);
            taskPendingHolder.submit(topicGroupId, task);
        }
    }

    /**
     * 顺序消息：按 orderKey 进入单线程分片执行器。
     */
    @Override
    public void consume2SendOrderly(List<TaskInfo> taskInfoLists) {
        String topicGroupId = GroupIdMappingUtils.getGroupIdByTaskInfo(CollUtil.getFirst(taskInfoLists.iterator()));
        long currentTimeMillis = System.currentTimeMillis();
        for (TaskInfo taskInfo : taskInfoLists) {
            logReceive(taskInfo, currentTimeMillis);
            taskPendingHolder.submitOrderly(topicGroupId, resolveOrderKey(taskInfo), context.getBean(Task.class).setTaskInfo(taskInfo));
        }
    }

    private String resolveOrderKey(TaskInfo taskInfo) {
        if (taskInfo == null) {
            return "default";
        }
        if (taskInfo.getOrderKey() != null && !taskInfo.getOrderKey().isBlank()) {
            return taskInfo.getOrderKey();
        }
        if (taskInfo.getBusinessOwner() != null && !taskInfo.getBusinessOwner().isBlank()
                && taskInfo.getBizId() != null && !taskInfo.getBizId().isBlank()) {
            return taskInfo.getBusinessOwner() + ":" + taskInfo.getBizId();
        }
        if (taskInfo.getBizId() != null && !taskInfo.getBizId().isBlank()) {
            return taskInfo.getBizId();
        }
        return taskInfo.getMessageId();
    }

    private void logReceive(TaskInfo taskInfo, long currentTimeMillis) {
        LogParam logParam = LogParam.builder()
                .bizType(LOG_BIZ_TYPE)
                .object(taskInfo)
                .timestamp(currentTimeMillis)
                .build();
        MsgPushLogRequest msgPushLogRequest = MsgPushLogRequest.builder()
                .bizType(MsgPushTypeEnum.SEND.getCode())
                .messageId(taskInfo.getMessageId())
                .messageTemplateId(taskInfo.getMessageTemplateId())
                .receiver(taskInfo.getReceiver())
                .state(MsgPushState.RECEIVE.getCode())
                .stateDescription(MsgPushState.RECEIVE.getDescription())
                .timestamp(System.currentTimeMillis())
                .build();
        logUtils.print(msgPushLogRequest);
        logUtils.print(logParam);
    }

    /**
     * 消费撤回消息后直接调用对应渠道执行撤回。
     */
    @Override
    public void consume2recall(RecallTaskInfo recallTaskInfo) {
        logUtils.print(LogParam.builder().bizType(LOG_BIZ_RECALL_TYPE).object(recallTaskInfo).build());
        handlerHolder.route(recallTaskInfo.getSendChannel()).recall(recallTaskInfo);
    }
}
