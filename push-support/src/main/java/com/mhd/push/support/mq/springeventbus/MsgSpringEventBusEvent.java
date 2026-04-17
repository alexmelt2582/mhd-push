package com.mhd.push.support.mq.springeventbus;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author zhao-hao-dong
 */
@Getter
public class MsgSpringEventBusEvent extends ApplicationEvent {
    private final MsgSpringEventSource msgSpringEventSource;

    public MsgSpringEventBusEvent(Object source, MsgSpringEventSource msgSpringEventSource) {
        super(source);
        this.msgSpringEventSource = msgSpringEventSource;
    }
}
