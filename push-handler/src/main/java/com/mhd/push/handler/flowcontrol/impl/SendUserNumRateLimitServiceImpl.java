package com.mhd.push.handler.flowcontrol.impl;

import com.google.common.util.concurrent.RateLimiter;
import com.mhd.push.common.domain.TaskInfo;
import com.mhd.push.handler.enums.RateLimitStrategy;
import com.mhd.push.handler.flowcontrol.FlowControlParam;
import com.mhd.push.handler.flowcontrol.FlowControlService;
import com.mhd.push.handler.flowcontrol.annotations.LocalRateLimit;

/**
 * @author zhao-hao-dong
 */
@LocalRateLimit(rateLimitStrategy = RateLimitStrategy.SEND_USER_NUM_RATE_LIMIT)
public class SendUserNumRateLimitServiceImpl implements FlowControlService {
    @Override
    public Double flowControl(TaskInfo taskInfo, FlowControlParam flowControlParam) {
        RateLimiter rateLimiter = flowControlParam.getRateLimiter();
        return rateLimiter.acquire(taskInfo.getReceiver().size());
    }
}
