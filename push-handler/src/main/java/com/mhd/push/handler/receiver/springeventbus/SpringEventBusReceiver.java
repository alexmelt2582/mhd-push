package com.mhd.push.handler.receiver.springeventbus;

import com.mhd.push.common.domain.RecallTaskInfo;
import com.mhd.push.common.domain.TaskInfo;
import com.mhd.push.handler.receiver.service.ConsumeService;
import com.mhd.push.support.constants.MessageQueuePipeline;
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
@ConditionalOnProperty(name = "mhd.mq.pipeline", havingValue = MessageQueuePipeline.SPRING_EVENT_BUS)
public class SpringEventBusReceiver {
    @Resource
    private ConsumeService consumeService;

    public void consume(List<TaskInfo> lists) {
        consumeService.consume2Send(lists);
    }

    public void recall(RecallTaskInfo recallTaskInfo) {
        consumeService.consume2recall(recallTaskInfo);
    }
}
