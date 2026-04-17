package com.mhd.push.support.mq.eventbus;

import com.mhd.push.common.domain.RecallTaskInfo;
import com.mhd.push.common.domain.TaskInfo;

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
