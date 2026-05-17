package com.mhd.push.engine.action;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson2.JSON;
import com.mhd.push.common.constant.CommonConstant;
import com.mhd.push.common.enums.MsgPushState;
import com.mhd.push.common.enums.SendTypeEnum;
import com.mhd.push.common.log.LogRecord;
import com.mhd.push.common.pipeline.BusinessProcess;
import com.mhd.push.common.pipeline.ProcessContext;
import com.mhd.push.common.utils.StringUtils;
import com.mhd.push.domain.model.task.TaskInfo;
import com.mhd.push.infra.config.service.ConfigService;
import com.mhd.push.infra.utils.LogUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 丢弃消息
 * 一般将需要丢弃的模板id写在分布式配置中心
 *
 * @author zhao-hao-dong
 */
@Service
public class DiscardAction implements BusinessProcess<TaskInfo> {
    private static final String DISCARD_MESSAGE_KEY = "discardMsgIds";
    @Resource
    private ConfigService configService;
    @Resource
    private LogUtils logUtils;

    @Override
    public void process(ProcessContext<TaskInfo> context) {
        TaskInfo taskInfo = context.getProcessModel();
        // 配置示例:	["1","2"]
        // 从配置中心获取丢弃的模板ID，若包含则丢弃
        List<String> discardTemplateIdList = JSON.parseArray(configService.getProperty(DISCARD_MESSAGE_KEY, CommonConstant.EMPTY_VALUE_JSON_ARRAY), String.class);
        String taskTemplateIdStr = String.valueOf(taskInfo.getTemplateId());
        if (CollUtil.isNotEmpty(discardTemplateIdList) && StringUtils.isNotBlank(taskTemplateIdStr) && discardTemplateIdList.contains(taskTemplateIdStr)) {
            LogRecord logRecord = LogRecord.build(SendTypeEnum.SEND, taskInfo, MsgPushState.DISCARD_DISCARD);
            logUtils.printWarn(logRecord);
            context.setNeedBreak(true);
        }
        LogRecord logRecord = LogRecord.build(SendTypeEnum.SEND, taskInfo, MsgPushState.DISCARD_MODULE_SUCCESS);
        logUtils.print(logRecord);
    }
}
