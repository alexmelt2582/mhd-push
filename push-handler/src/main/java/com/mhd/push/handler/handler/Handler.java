package com.mhd.push.handler.handler;

import com.mhd.push.common.domain.RecallTaskInfo;
import com.mhd.push.common.domain.TaskInfo;

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
