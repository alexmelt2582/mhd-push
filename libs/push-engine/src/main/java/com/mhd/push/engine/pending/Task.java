package com.mhd.push.engine.pending;

import com.mhd.push.common.pipeline.BasicResultVO;
import com.mhd.push.common.pipeline.ProcessContext;
import com.mhd.push.common.pipeline.ProcessController;
import com.mhd.push.common.pipeline.ProcessModel;
import com.mhd.push.domain.model.task.TaskInfo;
import com.mhd.push.engine.config.TaskPipelineConfig;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author zhao-hao-dong
 */
@Data
@Accessors(chain = true)
@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class Task implements Runnable {
    private TaskInfo taskInfo;
    @Autowired
    @Qualifier("handlerProcessController")
    private ProcessController processController;

    @Override
    public void run() {
        ProcessContext<ProcessModel> context = ProcessContext.builder()
                .processModel(taskInfo).code(TaskPipelineConfig.PIPELINE_HANDLER_CODE)
                .needBreak(false).response(BasicResultVO.success())
                .build();
        processController.process(context);
    }
}
