package com.mhd.push.publicapi.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 运维人工确认待确认消息时的请求参数。
 */
@Data
public class PendingConfirmConfirmDTO {
    /**
     * 发送执行记录在 Redis 中的主键。
     */
    @NotBlank(message = "executionKey 不能为空")
    private String executionKey;
}