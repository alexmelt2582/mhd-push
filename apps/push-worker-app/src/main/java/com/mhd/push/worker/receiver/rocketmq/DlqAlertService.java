package com.mhd.push.worker.receiver.rocketmq;

import com.mhd.push.domain.model.mq.MqDlqRecord;

/**
 * Internal alert service for DLQ records.
 * <p>
 * The alert target is operations/maintenance staff, not business callers.
 */
public interface DlqAlertService {

    /**
     * Notify ops that a DLQ record requires manual handling.
     */
    void alert(MqDlqRecord record);
}
