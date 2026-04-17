package com.mhd.push.support.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 消息模板信息
 */
@TableName(value = "message_template")
@Accessors(chain = true)
@Data
public class MessageTemplate {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 标题
     */
    @TableField(value = "name")
    private String name;

    /**
     * 当前消息审核状态： 10.待审核 20.审核成功 30.被拒绝
     */
    @TableField(value = "audit_status")
    private Integer auditStatus;

    /**
     * 工单ID
     */
    @TableField(value = "flow_id")
    private String flowId;

    /**
     * 当前消息状态：10.新建 20.停用 30.启用 40.等待发送 50.发送中 60.发送成功 70.发送失败
     */
    @TableField(value = "msg_status")
    private Integer msgStatus;

    /**
     * 定时任务Id (xxl-job-admin返回)
     */
    @TableField(value = "cron_task_id")
    private Long cronTaskId;

    /**
     * 定时发送人群的文件路径
     */
    @TableField(value = "cron_crowd_path")
    private String cronCrowdPath;

    /**
     * 期望发送时间：0:立即发送 定时任务以及周期任务:cron表达式
     */
    @TableField(value = "expect_push_time")
    private String expectPushTime;

    /**
     * 消息的发送ID类型：10. userId 20.did 30.手机号 40.openId 50.email 60.企业微信userId
     */
    @TableField(value = "id_type")
    private Integer idType;

    /**
     * 消息发送渠道：10.IM 20.Push 30.短信 40.Email 50.公众号 60.小程序 70.企业微信 80.钉钉机器人 90.钉钉工作通知 100.企业微信机器人 110.飞书机器人 110. 飞书应用消息
     */
    @TableField(value = "send_channel")
    private Integer sendChannel;

    /**
     * 10.运营类 20.技术类接口调用
     */
    @TableField(value = "template_type")
    private Integer templateType;

    /**
     * 10.通知类消息 20.营销类消息 30.验证码类消息
     */
    @TableField(value = "msg_type")
    private Integer msgType;

    /**
     * 10.夜间不屏蔽 20.夜间屏蔽 30.夜间屏蔽(次日早上9点发送)
     */
    @TableField(value = "shield_type")
    private Integer shieldType;

    /**
     * 消息内容 占位符用{$var}表示
     */
    @TableField(value = "msg_content")
    private String msgContent;

    /**
     * 发送账号 一个渠道下可存在多个账号
     */
    @TableField(value = "send_account")
    private Integer sendAccount;

    /**
     * 创建者
     */
    @TableField(value = "creator")
    private String creator;

    /**
     * 更新者
     */
    @TableField(value = "updator")
    private String updator;

    /**
     * 审核人
     */
    @TableField(value = "auditor")
    private String auditor;

    /**
     * 业务方团队
     */
    @TableField(value = "team")
    private String team;

    /**
     * 业务方
     */
    @TableField(value = "proposer")
    private String proposer;

    /**
     * 是否删除：0.不删除 1.删除
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

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
}