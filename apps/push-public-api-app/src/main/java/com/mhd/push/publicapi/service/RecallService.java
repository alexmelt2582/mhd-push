package com.mhd.push.publicapi.service;


import com.mhd.push.publicapi.domain.SendRequest;
import com.mhd.push.publicapi.domain.SendResultVO;

/**
 * 撤回接口
 */
public interface RecallService {

    /**
     * 根据 模板ID 或消息id 撤回消息
     * 如果只传入 messageTemplateId，则会撤回整个模板下发的消息
     * 如果还传入 recallMessageId，则优先撤回该 ids 的消息
     */
    SendResultVO recall(SendRequest sendRequest);
}
