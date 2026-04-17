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
@LocalRateLimit(rateLimitStrategy = RateLimitStrategy.REQUEST_RATE_LIMIT)
public class RequestRateLimitServiceImpl implements FlowControlService {
    @Override
    public Double flowControl(TaskInfo taskInfo, FlowControlParam flowControlParam) {
        RateLimiter rateLimiter = flowControlParam.getRateLimiter();
        return rateLimiter.acquire(1);
    }
}
