package com.mhd.push.handler.deduplication.service;

import com.mhd.push.handler.deduplication.DeduplicationParam;

/**
 * @author zhao-hao-dong
 */
public interface DeduplicationService {
    /**
     * 去重
     */
    void deduplication(DeduplicationParam param);
}
