package com.mhd.push.engine.handler;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SendExecutionStatus {
    SENDING("SENDING"),
    SUCCESS("SUCCESS"),
    FAIL("FAIL"),
    PENDING_CONFIRM("PENDING_CONFIRM");

    private final String code;
}