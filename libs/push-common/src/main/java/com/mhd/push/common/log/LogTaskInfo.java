package com.mhd.push.common.log;

import java.util.Set;

/**
 * 供日志模块读取任务最小必要字段，避免 common 反向依赖 domain。
 */
public interface LogTaskInfo {

    String getTraceId();

    Long getTemplateId();

    Integer getTemplateType();

    Set<String> getReceiver();

    String getCallbackUrl();
}