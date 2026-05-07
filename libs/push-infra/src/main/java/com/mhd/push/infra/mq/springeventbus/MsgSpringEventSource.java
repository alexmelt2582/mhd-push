package com.mhd.push.infra.mq.springeventbus;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @author zhao-hao-dong
 */
@Data
@Builder
public class MsgSpringEventSource implements Serializable {
    private String topic;
    private String jsonValue;
    private String tagId;
}
