package com.mhd.push.web.api.service;

import com.mhd.push.web.api.domain.TraceResponse;

/**
 * 链路查询接口
 *
 * @author zhao-hao-dong

 */
public interface TraceService {
    /**
     * 基于消息 ID 查询 链路结果
     */
    TraceResponse traceByMessageId(String messageId);
}
