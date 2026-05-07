package com.mhd.push.publicapi.domain;

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
     * 发送结果回调地址（可选）。
     */
    private String callbackUrl;

    /**
     * 消息相关的参数
     * 必传
     */
    private List<MessageParam> messageParamList;
}
