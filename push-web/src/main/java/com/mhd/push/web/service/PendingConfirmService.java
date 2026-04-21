package com.mhd.push.web.service;

import com.mhd.push.handler.handler.SendExecutionGuardService;
import com.mhd.push.handler.handler.SendExecutionRecord;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 待确认消息运维服务。
 */
@Service
public class PendingConfirmService {

    @Resource
    private SendExecutionGuardService sendExecutionGuardService;

    public List<SendExecutionRecord> listPendingRecords(Integer limit) {
        return sendExecutionGuardService.listPendingConfirmRecords(limit == null ? 20 : limit);
    }

    public List<SendExecutionRecord> queryByMessageId(String messageId) {
        return sendExecutionGuardService.queryPendingConfirmByMessageId(messageId);
    }

    public boolean confirmSuccess(String executionKey) {
        return sendExecutionGuardService.confirmSuccess(executionKey);
    }

    public boolean confirmFail(String executionKey) {
        return sendExecutionGuardService.confirmFail(executionKey);
    }
}