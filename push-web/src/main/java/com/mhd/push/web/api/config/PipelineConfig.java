package com.mhd.push.web.api.config;

import com.mhd.push.common.pipeline.ProcessController;
import com.mhd.push.common.pipeline.ProcessTemplate;
import com.mhd.push.common.enums.SendTypeEnum;
import com.mhd.push.web.api.action.send.SendAfterCheckAction;
import com.mhd.push.web.api.action.send.SendAssembleAction;
import com.mhd.push.web.api.action.send.SendMqAction;
import com.mhd.push.web.api.action.send.SendPreCheckAction;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * api层的pipeline配置类
 *
 * @author zhao-hao-dong

 */
@Configuration
public class PipelineConfig {
    @Resource
    private SendPreCheckAction sendPreCheckAction;
    @Resource
    private SendAssembleAction sendAssembleAction;
    @Resource
    private SendAfterCheckAction sendAfterCheckAction;
    @Resource
    private SendMqAction sendMqAction;
    //@Resource
    //private RecallAssembleAction recallAssembleAction;
    //@Resource
    //private RecallMqAction recallMqAction;

    /**
     * 普通发送执行流程
     * 1. 前置参数校验
     * 2. 组装参数
     * 3. 后置参数校验
     * 4. 发送消息至MQ
     */
    @Bean("commonSendTemplate")
    public ProcessTemplate commonSendTemplate() {
        ProcessTemplate processTemplate = new ProcessTemplate();
        processTemplate.setProcessList(Arrays.asList(sendPreCheckAction, sendAssembleAction,
                sendAfterCheckAction, sendMqAction));
        return processTemplate;
    }

    ///**
    // * 消息撤回执行流程
    // * 1.组装参数
    // * 2.发送MQ
    // *
    // * @return
    // */
    //@Bean("recallMessageTemplate")
    //public ProcessTemplate recallMessageTemplate() {
    //    ProcessTemplate processTemplate = new ProcessTemplate();
    //    processTemplate.setProcessList(Arrays.asList(recallAssembleAction, recallMqAction));
    //    return processTemplate;
    //}

    /**
     * pipeline流程控制器
     */
    @Bean("apiProcessController")
    public ProcessController apiProcessController() {
        ProcessController processController = new ProcessController();
        Map<String, ProcessTemplate> templateConfig = new HashMap<>(4);
        templateConfig.put(SendTypeEnum.SEND.getCode(), commonSendTemplate());
        //templateConfig.put(BusinessCode.RECALL.getCode(), recallMessageTemplate());
        processController.setTemplateConfig(templateConfig);
        return processController;
    }
}
