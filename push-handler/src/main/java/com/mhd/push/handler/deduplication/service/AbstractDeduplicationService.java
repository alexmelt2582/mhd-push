package com.mhd.push.handler.deduplication.service;

import cn.hutool.core.collection.CollUtil;
import com.mhd.push.common.domain.AnchorInfo;
import com.mhd.push.common.domain.MsgPushLogRequest;
import com.mhd.push.common.domain.TaskInfo;
import com.mhd.push.common.enums.MsgPushState;
import com.mhd.push.common.enums.MsgPushTypeEnum;
import com.mhd.push.handler.deduplication.DeduplicationHolder;
import com.mhd.push.handler.deduplication.DeduplicationParam;
import com.mhd.push.handler.deduplication.limit.LimitService;
import com.mhd.push.support.utils.LogUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import java.util.Set;

/**
 * @author zhao-hao-dong
 */
@Slf4j
public abstract class AbstractDeduplicationService implements DeduplicationService {
    protected Integer deduplicationType;

    protected LimitService limitService;

    @Resource
    private DeduplicationHolder deduplicationHolder;
    @Resource
    private LogUtils logUtils;

    @PostConstruct
    private void init() {
        deduplicationHolder.putService(deduplicationType, this);
    }

    @Override
    public void deduplication(DeduplicationParam param) {
        TaskInfo taskInfo = param.getTaskInfo();

        Set<String> filterReceiver = limitService.limitFilter(this, taskInfo, param);

        // 剔除符合去重条件的用户
        if (CollUtil.isNotEmpty(filterReceiver)) {
            taskInfo.getReceiver().removeAll(filterReceiver);
            MsgPushLogRequest msgPushLogRequest = MsgPushLogRequest.builder()
                    .bizType(MsgPushTypeEnum.SEND.getCode())
                    .messageId(taskInfo.getMessageId())
                    .messageTemplateId(taskInfo.getMessageTemplateId())
                    .receiver(taskInfo.getReceiver())
                    .state(param.getMsgPushState().getCode())
                    .stateDescription(param.getMsgPushState().getDescription())
                    .timestamp(System.currentTimeMillis())
                    .build();
            logUtils.print(msgPushLogRequest);
        }
    }

    /**
     * 构建去重的Key
     */
    public abstract String deduplicationSingleKey(TaskInfo taskInfo, String receiver);
}
