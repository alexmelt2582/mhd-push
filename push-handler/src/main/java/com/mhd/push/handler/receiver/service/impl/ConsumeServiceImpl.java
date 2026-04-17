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

    @Override
    public void consume2Send(List<TaskInfo> taskInfoLists) {
        String topicGroupId = GroupIdMappingUtils.getGroupIdByTaskInfo(CollUtil.getFirst(taskInfoLists.iterator()));
        long currentTimeMillis = System.currentTimeMillis();
        for (TaskInfo taskInfo : taskInfoLists) {
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
            Task task = context.getBean(Task.class).setTaskInfo(taskInfo);
            taskPendingHolder.route(topicGroupId).execute(task);
        }
    }

    @Override
    public void consume2recall(RecallTaskInfo recallTaskInfo) {
        logUtils.print(LogParam.builder().bizType(LOG_BIZ_RECALL_TYPE).object(recallTaskInfo).build());
        handlerHolder.route(recallTaskInfo.getSendChannel()).recall(recallTaskInfo);
    }
}
