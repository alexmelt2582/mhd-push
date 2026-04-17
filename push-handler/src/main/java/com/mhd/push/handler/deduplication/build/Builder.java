package com.mhd.push.handler.deduplication.build;

import com.mhd.push.common.domain.TaskInfo;
import com.mhd.push.handler.deduplication.DeduplicationParam;

/**
 * @author zhao-hao-dong
 */
public interface Builder {
    String DEDUPLICATION_CONFIG_PRE = "deduplication_";

    /**
     * 根据配置构建去重参数
     */
    DeduplicationParam build(String deduplication, TaskInfo taskInfo);
}
