package com.mhd.push.handler.action;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.mhd.push.common.constant.CommonConstant;
import com.mhd.push.common.domain.LogRecord;
import com.mhd.push.common.domain.TaskInfo;
import com.mhd.push.common.enums.MsgPushState;
import com.mhd.push.common.enums.SendTypeEnum;
import com.mhd.push.common.pipeline.BusinessProcess;
import com.mhd.push.common.pipeline.ProcessContext;
import com.mhd.push.support.service.ConfigService;
import com.mhd.push.support.utils.LogUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

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
        JSONArray array = JSON.parseArray(configService.getProperty(DISCARD_MESSAGE_KEY, CommonConstant.EMPTY_VALUE_JSON_ARRAY));
        if (array.contains(String.valueOf(taskInfo.getTemplateId()))) {
            LogRecord logRecord = LogRecord.build(SendTypeEnum.SEND, taskInfo, MsgPushState.DISCARD);
            logUtils.print(logRecord);
            context.setNeedBreak(true);
        }
    }
}
