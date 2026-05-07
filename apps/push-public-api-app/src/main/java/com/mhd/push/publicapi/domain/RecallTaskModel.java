package com.mhd.push.publicapi.domain;

import com.mhd.push.common.pipeline.ProcessModel;
import com.mhd.push.domain.model.task.RecallTaskInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 发送消息任务模型
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RecallTaskModel implements ProcessModel {

    /**
     * 消息模板Id
     */
    private Long messageTemplateId;

    /**
     * 需要撤回的消息ids
     */
    private List<String> recallMessageId;

    /**
     * 撤回任务 domain
     */
    private RecallTaskInfo recallTaskInfo;
}
