package com.mhd.push.handler.deduplication.service;

import com.mhd.push.common.domain.TaskInfo;
import com.mhd.push.handler.deduplication.DeduplicationParam;

/**
 * @author zhao-hao-dong
 */
public interface DeduplicationService {
    /**
     * 获取去重类型
     * @see com.mhd.push.common.enums.DeduplicationType
     * @return 去重类型
     */
    Integer getDeduplicationType();

    /**
     * 根据去重配置和任务信息构建去重对象
     * @param deduplication 去重配置
     * @param taskInfo 任务信息
     * @return 去重对象
     */
    DeduplicationParam build(String deduplication, TaskInfo taskInfo);

    /**
     * 根据去重对象DeduplicationParam去重
     * @param param 去重对象
     */
    void deduplication(DeduplicationParam param);
}
