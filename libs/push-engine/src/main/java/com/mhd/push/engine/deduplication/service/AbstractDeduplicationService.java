package com.mhd.push.engine.deduplication.service;

import cn.hutool.core.collection.CollUtil;
import com.mhd.push.common.log.LogRecord;
import com.mhd.push.domain.model.task.TaskInfo;
import com.mhd.push.common.enums.SendTypeEnum;
import com.mhd.push.engine.deduplication.DeduplicationHolder;
import com.mhd.push.engine.deduplication.DeduplicationParam;
import com.mhd.push.infra.utils.LogUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import java.util.Set;

/**
 * @author zhao-hao-dong
 */
@Slf4j
public abstract class AbstractDeduplicationService implements DeduplicationService {
    protected String DEDUPLICATION_CONFIG_PRE = "deduplication_";

    @Resource
    private DeduplicationHolder deduplicationHolder;
    @Resource
    private LogUtils logUtils;

    @PostConstruct
    private void init() {
        deduplicationHolder.putService(getDeduplicationType(), this);
    }

    @Override
    public void deduplication(DeduplicationParam param) {
        TaskInfo taskInfo = param.getTaskInfo();

        Set<String> filterReceiver = limitFilter(taskInfo, param);

        // 剔除符合去重条件的用户
        if (CollUtil.isNotEmpty(filterReceiver)) {
            taskInfo.getReceiver().removeAll(filterReceiver);
            LogRecord logRecord = LogRecord.build(SendTypeEnum.SEND, taskInfo, param.getMsgPushState());
            logUtils.printWarn(logRecord);
        }
    }

    /**
     * 构建去重的Key
     */
    public abstract String deduplicationSingleKey(TaskInfo taskInfo, String receiver);

    /**
     * 去重限制
     *
     * @param taskInfo 任务
     * @param param    去重参数
     * @return 返回不符合条件
     */
    public abstract Set<String> limitFilter(TaskInfo taskInfo, DeduplicationParam param);
}
