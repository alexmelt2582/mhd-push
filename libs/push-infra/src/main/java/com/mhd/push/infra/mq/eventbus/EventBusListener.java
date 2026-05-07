package com.mhd.push.infra.mq.eventbus;

import com.mhd.push.domain.model.task.RecallTaskInfo;
import com.mhd.push.domain.model.task.TaskInfo;

import java.util.List;

/**
 * @author zhao-hao-dong
 */
public interface EventBusListener {

    /**
     * 消费消息
     */
    void consume(List<TaskInfo> lists);

    /**
     * 撤回消息
     */
    void recall(RecallTaskInfo recallTaskInfo);
}
