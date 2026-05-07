package com.mhd.push.publicapi.config;

import com.mhd.push.common.enums.SendTypeEnum;
import com.mhd.push.common.pipeline.ProcessController;
import com.mhd.push.common.pipeline.ProcessTemplate;
import com.mhd.push.publicapi.action.send.SendAfterCheckAction;
import com.mhd.push.publicapi.action.send.SendAssembleAction;
import com.mhd.push.publicapi.action.send.SendMqAction;
import com.mhd.push.publicapi.action.send.SendPreCheckAction;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * public-api 责任链配置类。
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
     * 构建普通发送执行流程。
     *
     * @return 发送流程模板
     */
    @Bean("commonSendTemplate")
    public ProcessTemplate commonSendTemplate() {
        ProcessTemplate processTemplate = new ProcessTemplate();
        // 1. 依次串联前置校验、模板组装、后置校验和 MQ 投递。
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
     * 注册 public-api 使用的流程控制器。
     *
     * @return 流程控制器
     */
    @Bean("apiProcessController")
    public ProcessController apiProcessController() {
        ProcessController processController = new ProcessController();
        Map<String, ProcessTemplate> templateConfig = new HashMap<>(4);
        // 1. 将业务编码映射到对应的责任链模板。
        templateConfig.put(SendTypeEnum.SEND.getCode(), commonSendTemplate());
        //templateConfig.put(BusinessCode.RECALL.getCode(), recallMessageTemplate());
        processController.setTemplateConfig(templateConfig);
        return processController;
    }
}
