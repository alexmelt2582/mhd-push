package com.mhd.push.engine.receiver.impl;

import cn.hutool.core.collection.CollUtil;
import com.mhd.push.common.log.LogParam;
import com.mhd.push.common.log.LogRecord;
import com.mhd.push.domain.model.task.RecallTaskInfo;
import com.mhd.push.domain.model.task.TaskInfo;
import com.mhd.push.common.enums.MsgPushState;
import com.mhd.push.common.enums.SendTypeEnum;
import com.mhd.push.engine.handler.HandlerHolder;
import com.mhd.push.engine.utils.GroupIdMappingUtils;
import com.mhd.push.engine.pending.Task;
import com.mhd.push.engine.pending.TaskPendingHolder;
import com.mhd.push.engine.receiver.ConsumeService;
import com.mhd.push.infra.utils.LogUtils;
import jakarta.annotation.Resource;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

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
            taskPendingHolder.submitOrderly(topicGroupId, buildOrderKey(taskInfo), context.getBean(Task.class).setTaskInfo(taskInfo));
        }
    }

    private String buildOrderKey(TaskInfo taskInfo) {
        if (Objects.isNull(taskInfo)) return null;
        StringBuilder sb = new StringBuilder();
        if (taskInfo.getBusinessOwner() != null && !taskInfo.getBusinessOwner().isBlank()
                && taskInfo.getOrderingKey() != null && !taskInfo.getOrderingKey().isBlank()) {
            sb.append(taskInfo.getBusinessOwner())
                    .append(":")
                    .append(taskInfo.getOrderingKey());
        }
        return sb.toString();
    }

    private void logReceive(TaskInfo taskInfo, long currentTimeMillis) {
        LogParam logParam = LogParam.builder()
                .bizType(LOG_BIZ_TYPE)
                .object(taskInfo)
                .timestamp(currentTimeMillis)
                .build();
        LogRecord logRecord = LogRecord.build(SendTypeEnum.SEND, taskInfo, MsgPushState.RECEIVE_MQ_MODULE_SUCCESS);
        logUtils.print(logParam);
        logUtils.print(logRecord);
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
