package com.mhd.push.web.api.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author zhao-hao-dong

 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class BatchSendRequest {
    /**
     * 执行业务类型
     * 必传
     */
    private String code;

    /**
     * 消息模板Id
     * 必传
     */
    private Long messageTemplateId;

    /**
     * 幂等键（建议由调用方生成唯一值）
     */
    private String idempotencyKey;

    /**
     * 消息相关的参数
     * 必传
     */
    private List<MessageParam> messageParamList;
}
