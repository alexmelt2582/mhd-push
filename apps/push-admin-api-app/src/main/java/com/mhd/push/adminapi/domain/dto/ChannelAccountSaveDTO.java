package com.mhd.push.adminapi.domain.dto;

import lombok.Data;

/**
 * 渠道账号信息
 */
@Data
public class ChannelAccountSaveDTO {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 账号名称
     */
    private String name;

    /**
     * 消息发送渠道：10.IM 20.Push 30.短信 40.Email 50.公众号 60.小程序 70.企业微信 80.钉钉机器人 90.钉钉工作通知 100.企业微信机器人 110.飞书机器人 110. 飞书应用消息 
     */
    private Integer sendChannel;

    /**
     * 账号配置
     */
    private String accountConfig;
}