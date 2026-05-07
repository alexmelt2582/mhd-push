package com.mhd.push.worker.receiver.eventbus;

import com.google.common.eventbus.Subscribe;
import com.mhd.push.common.constant.MessageQueuePipeline;
import com.mhd.push.domain.model.task.RecallTaskInfo;
import com.mhd.push.domain.model.task.TaskInfo;
import com.mhd.push.engine.receiver.ConsumeService;
import com.mhd.push.engine.receiver.MessageReceiver;
import com.mhd.push.infra.mq.eventbus.EventBusListener;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author zhao-hao-dong
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "mhd.mq.pipeline", havingValue = MessageQueuePipeline.EVENT_BUS)
public class EventBusReceiver implements EventBusListener, MessageReceiver {
    @Resource
    private ConsumeService consumeService;

    @Override
    @Subscribe
    public void consume(List<TaskInfo> lists) {
        consumeService.consume2Send(lists);
    }

    @Override
    @Subscribe
    public void recall(RecallTaskInfo recallTaskInfo) {
        consumeService.consume2recall(recallTaskInfo);
    }
}
