package com.mhd.push.engine.action;

import cn.hutool.core.util.ObjectUtil;
import com.google.common.collect.Sets;
import com.mhd.push.common.enums.MsgPushState;
import com.mhd.push.common.enums.SendTypeEnum;
import com.mhd.push.common.log.LogRecord;
import com.mhd.push.domain.model.task.TaskInfo;
import com.mhd.push.common.enums.ChannelType;
import com.mhd.push.common.pipeline.BusinessProcess;
import com.mhd.push.common.pipeline.ProcessContext;
import com.mhd.push.engine.handler.Handler;
import com.mhd.push.engine.handler.HandlerHolder;
import com.mhd.push.infra.utils.LogUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 发送消息，路由到对应的渠道下发消息
 *
 * @author zhao-hao-dong
 */
@Service
public class SendMessageAction implements BusinessProcess<TaskInfo> {
    @Resource
    private HandlerHolder handlerHolder;
    @Resource
    private LogUtils logUtils;

    @Override
    public void process(ProcessContext<TaskInfo> context) {
        TaskInfo taskInfo = context.getProcessModel();
        Handler route = handlerHolder.route(taskInfo.getSendChannel());
        if(route == null) {
            LogRecord logRecord = LogRecord.build(SendTypeEnum.SEND, taskInfo, MsgPushState.CHANNEL_ROUTE_FAIL);
            logUtils.printError(logRecord);
            context.setNeedBreak(true);
            return;
        }
        LogRecord logRecord = LogRecord.build(SendTypeEnum.SEND, taskInfo, MsgPushState.CHANNEL_ROUTE_SUCCESS);
        logUtils.print(logRecord);
        // 微信小程序&服务号只支持单人推送，为了后续逻辑统一处理，于是在这做了单发处理
        if (ChannelType.MINI_PROGRAM.getCode().equals(taskInfo.getSendChannel())
                || ChannelType.OFFICIAL_ACCOUNT.getCode().equals(taskInfo.getSendChannel())
                || ChannelType.ALIPAY_MINI_PROGRAM.getCode().equals(taskInfo.getSendChannel())) {
            TaskInfo taskClone = ObjectUtil.cloneByStream(taskInfo);
            for (String receiver : taskInfo.getReceiver()) {
                taskClone.setReceiver(Sets.newHashSet(receiver));
               route.doHandler(taskClone);
            }
            return;
        }
        route.doHandler(taskInfo);
    }
}
