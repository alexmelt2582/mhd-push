package com.mhd.push.domain.model.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 飞书 机器人 账号信息
 *
 * @author zhao-hao-dong
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FeiShuRobotAccount {
    /**
     * 自定义群机器人中的 webhook
     */
    private String webhook;

}
