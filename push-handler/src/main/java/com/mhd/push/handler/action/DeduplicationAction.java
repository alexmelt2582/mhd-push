package com.mhd.push.handler.action;

import cn.hutool.core.collection.CollUtil;
import com.mhd.push.common.constant.CommonConstant;
import com.mhd.push.common.domain.TaskInfo;
import com.mhd.push.common.enums.DeduplicationType;
import com.mhd.push.common.enums.EnumUtil;
import com.mhd.push.common.pipeline.BusinessProcess;
import com.mhd.push.common.pipeline.ProcessContext;
import com.mhd.push.handler.deduplication.DeduplicationHolder;
import com.mhd.push.handler.deduplication.DeduplicationParam;
import com.mhd.push.handler.deduplication.service.DeduplicationService;
import com.mhd.push.support.service.ConfigService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 去重服务
 * 1. 根据相同内容N分钟去重（SlideWindowLimitService）
 * 2. 相同的渠道一天内频次去重（SimpleLimitService）
 *
 * @author zhao-hao-dong
 */
@Service
public class DeduplicationAction implements BusinessProcess<TaskInfo> {
    private static final String DEDUPLICATION_RULE_KEY = "deduplicationRule";

    @Resource
    private ConfigService configService;
    @Resource
    private DeduplicationHolder deduplicationHolder;

    @Override
    public void process(ProcessContext<TaskInfo> context) {
        TaskInfo taskInfo = context.getProcessModel();

        // 配置样例：{"deduplication_10":{"num":1,"time":300},"deduplication_20":{"num":5}}
        // 从配置中心读取整段去重配置。
        String deduplicationConfig = configService.getProperty(DEDUPLICATION_RULE_KEY, CommonConstant.EMPTY_JSON_OBJECT);
        applyConfiguredDeduplicationRules(taskInfo, deduplicationConfig);

        if (CollUtil.isEmpty(taskInfo.getReceiver())) {
            context.setNeedBreak(true);
        }
    }

    /**
     * 按去重类型顺:
     */
    private void applyConfiguredDeduplicationRules(TaskInfo taskInfo, String deduplicationConfig) {
        List<Integer> deduplicationTypes = EnumUtil.getCodeList(DeduplicationType.class);
        for (Integer deduplicationType : deduplicationTypes) {
            DeduplicationService deduplicationService = deduplicationHolder.selectService(deduplicationType);
            if (Objects.isNull(deduplicationService)) {
                continue;
            }
            DeduplicationParam param = deduplicationService.build(deduplicationConfig, taskInfo);
            if (Objects.nonNull(param)) {
                deduplicationService.deduplication(param);
            }
        }
    }
}
