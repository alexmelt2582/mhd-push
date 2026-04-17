package com.mhd.push.support.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 渠道账号信息
 */
@TableName(value = "channel_account")
@Data
public class ChannelAccount {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 账号名称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 消息发送渠道：10.IM 20.Push 30.短信 40.Email 50.公众号 60.小程序 70.企业微信 80.钉钉机器人 90.钉钉工作通知 100.企业微信机器人 110.飞书机器人 110. 飞书应用消息
     */
    @TableField(value = "send_channel")
    private Integer sendChannel;

    /**
     * 账号配置
     */
    @TableField(value = "account_config")
    private String accountConfig;

    /**
     * 拥有者
     */
    @TableField(value = "creator")
    private String creator;

    /**
     * 创建时间
     */
    @TableField(value = "created")
    private Integer created;

    /**
     * 更新时间
     */
    @TableField(value = "updated")
    private Integer updated;

    /**
     * 是否删除：0.不删除 1.删除
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;
}