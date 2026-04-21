package com.mhd.push.handler.receiver.service;

import com.mhd.push.common.domain.RecallTaskInfo;
import com.mhd.push.common.domain.TaskInfo;

import java.util.List;

/**
 * 消息消费服务
 *
 * @author zhao-hao-dong
 */
public interface ConsumeService {
    /**
     * 从MQ拉到消息进行消费，发送消息
     */
    void consume2Send(List<TaskInfo> taskInfoLists);

    /**
        * 从MQ拉到顺序消息后，按 orderKey 串行执行发送。
     */
        void consume2SendOrderly(List<TaskInfo> taskInfoLists);

    /**
     * 从MQ拉到消息进行消费，撤回消息
     * 如果有 recallMessageId ，则优先撤回 recallMessageId
     * 如果没有 recallMessageId ，则撤回整个模板的消息
     */
    void consume2recall(RecallTaskInfo recallTaskInfo);
}
