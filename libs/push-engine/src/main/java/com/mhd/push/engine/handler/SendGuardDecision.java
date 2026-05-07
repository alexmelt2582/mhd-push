package com.mhd.push.engine.handler;

public enum SendGuardDecision {
    /**
     * 当前节点可以继续真正调用第三方。
     */
    NEW_SEND,
    /**
     * 此消息已经在本地确认成功，直接跳过。
     */
    ALREADY_SUCCESS,
    /**
     * 此消息处于待确认状态，不能再次发送到第三方。
     */
    PENDING_CONFIRM
}