package com.mhd.push.handler.flowcontrol;

import com.mhd.push.common.domain.TaskInfo;

/**
 * 流量控制服务
 *
 * @author zhao-hao-dong
 */
public interface FlowControlService {
    /**
     * 根据渠道进行流量控制
     */
    Double flowControl(TaskInfo taskInfo, FlowControlParam flowControlParam);
}
