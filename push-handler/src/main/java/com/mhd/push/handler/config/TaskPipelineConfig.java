package com.mhd.push.handler.config;

import com.mhd.push.common.pipeline.ProcessController;
import com.mhd.push.common.pipeline.ProcessTemplate;
import com.mhd.push.handler.action.*;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * handler层的pipeline配置类
 *
 * @author zhao-hao-dong
 */
@Configuration
public class TaskPipelineConfig {
    public static final String PIPELINE_HANDLER_CODE = "handler";

    @Resource
    private DiscardAction discardAction;
    @Resource
    private ShieldAction shieldAction;
    @Resource
    private DeduplicationAction deduplicationAction;
    @Resource
    private SensWordsAction sensWordsAction;
    @Resource
    private SendMessageAction sendMessageAction;

    /**
     * 消息从MQ消费的流程
     * 0.丢弃消息
     * 1.屏蔽消息
     * 2.通用去重功能
     * 3.发送消息
     */
    @Bean("taskTemplate")
    public ProcessTemplate taskTemplate() {
        ProcessTemplate processTemplate = new ProcessTemplate();
        processTemplate.setProcessList(Arrays.asList(discardAction, shieldAction, deduplicationAction,
                sensWordsAction, sendMessageAction));
        return processTemplate;
    }

    /**
     * pipeline流程控制器
     * 后续扩展则加BusinessCode和ProcessTemplate
     */
    @Bean("handlerProcessController")
    public ProcessController processController() {
        ProcessController processController = new ProcessController();
        Map<String, ProcessTemplate> templateConfig = new HashMap<>(4);
        templateConfig.put(PIPELINE_HANDLER_CODE, taskTemplate());
        processController.setTemplateConfig(templateConfig);
        return processController;
    }
}
