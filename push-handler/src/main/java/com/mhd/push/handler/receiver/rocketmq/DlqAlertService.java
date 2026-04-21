package com.mhd.push.handler.receiver.rocketmq;

import com.mhd.push.common.domain.MqDlqRecord;

/**
 * Internal alert service for DLQ records.
 *
 * The alert target is operations/maintenance staff, not business callers.
 */
public interface DlqAlertService {

    /**
     * Notify ops that a DLQ record requires manual handling.
     */
    void alert(MqDlqRecord record);
}
