package com.mhd.push.handler.action;

import cn.hutool.core.util.ObjectUtil;
import com.google.common.collect.Sets;
import com.mhd.push.common.domain.TaskInfo;
import com.mhd.push.common.enums.ChannelType;
import com.mhd.push.common.pipeline.BusinessProcess;
import com.mhd.push.common.pipeline.ProcessContext;
import com.mhd.push.handler.handler.HandlerHolder;
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

    @Override
    public void process(ProcessContext<TaskInfo> context) {
        TaskInfo taskInfo = context.getProcessModel();
        // 微信小程序&服务号只支持单人推送，为了后续逻辑统一处理，于是在这做了单发处理
        if (ChannelType.MINI_PROGRAM.getCode().equals(taskInfo.getSendChannel())
                || ChannelType.OFFICIAL_ACCOUNT.getCode().equals(taskInfo.getSendChannel())
                || ChannelType.ALIPAY_MINI_PROGRAM.getCode().equals(taskInfo.getSendChannel())) {
            TaskInfo taskClone = ObjectUtil.cloneByStream(taskInfo);
            for (String receiver : taskInfo.getReceiver()) {
                taskClone.setReceiver(Sets.newHashSet(receiver));
                handlerHolder.route(taskInfo.getSendChannel()).doHandler(taskClone);
            }
            return;
        }
        handlerHolder.route(taskInfo.getSendChannel()).doHandler(taskInfo);
    }
}
