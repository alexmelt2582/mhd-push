package com.mhd.push.handler.flowcontrol;

/**
 * 限流异常。
 */
public class FlowControlException extends RuntimeException {
    public FlowControlException(String message) {
        super(message);
    }
}