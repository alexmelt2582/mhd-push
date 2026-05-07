package com.mhd.push.engine.handler;

import com.mhd.push.domain.model.task.RecallTaskInfo;
import com.mhd.push.domain.model.task.TaskInfo;

/**
 * 消息处理器
 *
 * @author zhao-hao-dong
 */
public interface Handler {
    /**
     * 处理器
     */
    void doHandler(TaskInfo taskInfo);

    /**
     * 撤回消息
     */
    void recall(RecallTaskInfo recallTaskInfo);
}
