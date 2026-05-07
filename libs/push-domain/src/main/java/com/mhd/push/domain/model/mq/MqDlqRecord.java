package com.mhd.push.domain.model.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DLQ消息记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqDlqRecord implements Serializable {
    private String traceId;
    private String businessOwner;
    private String topic;
    private String tagId;
    private String orderKey;
    private Integer reconsumeTimes;
    private Integer maxReconsumeTimes;
    private String payload;
    private String errorReason;
    private String status;
    private Long createdAt;
    private Long compensatedAt;
}
