package com.mhd.push.engine.flowcontrol;

/**
 * 限流异常。
 */
public class FlowControlException extends RuntimeException {
    public FlowControlException(String message) {
        super(message);
    }
}