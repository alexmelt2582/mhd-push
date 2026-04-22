package com.mhd.push.web.api.domain;

import com.mhd.push.common.domain.TaskInfo;
import com.mhd.push.common.pipeline.ProcessModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 发送消息任务模型
 *
 * @author zhao-hao-dong
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SendTaskModel implements ProcessModel {
    /**
     * 消息模板Id
     */
    private Long templateId;

    /**
     * 排序Key
     */
    private String orderingKey;
    /**
     * 请求参数
     */
    private List<MessageParam> messageParamList;

    /**
     * 发送任务的信息
     */
    private List<TaskInfo> taskInfo;
}
