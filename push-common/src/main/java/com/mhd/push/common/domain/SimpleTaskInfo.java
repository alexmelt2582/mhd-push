package com.mhd.push.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zhao-hao-dong
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimpleTaskInfo {
    /**
     * 对外返回给调用方的唯一链路追踪 ID。
     */
    private String messageId;

    public static SimpleTaskInfo fromTaskInfo(TaskInfo taskInfo) {
        return new SimpleTaskInfo(taskInfo == null ? null : taskInfo.getTraceId());
    }
}
