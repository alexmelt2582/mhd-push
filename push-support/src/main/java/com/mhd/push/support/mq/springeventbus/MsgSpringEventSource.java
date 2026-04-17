package com.mhd.push.support.mq.springeventbus;

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
