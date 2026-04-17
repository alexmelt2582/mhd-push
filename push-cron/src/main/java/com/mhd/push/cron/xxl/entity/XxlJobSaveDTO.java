package com.mhd.push.cron.xxl.entity;

import com.mhd.push.cron.xxl.enums.ExecutorBlockStrategyEnum;
import com.mhd.push.cron.xxl.enums.ExecutorRouteStrategyEnum;
import com.mhd.push.cron.xxl.enums.MisfireStrategyEnum;
import com.mhd.push.cron.xxl.enums.ScheduleTypeEnum;
import lombok.Data;

/**
 * @author zhao-hao-dong
 **/
@Data
public class XxlJobSaveDTO {
    /**
     * 任务 ID
     */
    private Integer id;
    /**
     * 任务组 ID，对应执行器的 ID
     */
    private Integer jobGroup;
    /**
     * 任务描述
     */
    private String jobDesc;
    /**
     * 负责人
     */
    private String author;
    /**
     * 报警邮件
     */
    private String alarmEmail;
    /**
     * 调度类型（NONE/CRON/FIX_RATE）
     */
    private ScheduleTypeEnum scheduleType = ScheduleTypeEnum.CRON;
    /**
     * 调度配置
     */
    private String scheduleConf;
    /**
     * 调度配置
     */
    private String cronGenDisplay;
    /**
     * 调度配置
     */
    private String scheduleConfCRON;
    private String scheduleConfFIXRATE;
    private String scheduleConfFIXDELAY;
    /**
     * 运行模式
     */
    private String glueType = "BEAN";
    /**
     * 执行器任务处理器 JobHandler
     */
    private String executorHandler;
    /**
     * 执行器任务参数
     */
    private String executorParam;
    /**
     * 路由策略，默认第一个
     */
    private ExecutorRouteStrategyEnum executorRouteStrategy = ExecutorRouteStrategyEnum.FIRST;
    /**
     * 子任务 ID，多个逗号分隔
     */
    private String childJobId;
    /**
     * 调度过期策略
     */
    private MisfireStrategyEnum misfireStrategy = MisfireStrategyEnum.DO_NOTHING;
    /**
     * 阻塞处理策略，默认是单机串行
     */
    private ExecutorBlockStrategyEnum executorBlockStrategy = ExecutorBlockStrategyEnum.SERIAL_EXECUTION;
    /**
     * 超时时间，单位秒，大于零时生效
     */
    private Integer executorTimeout = 0;
    /**
     * 失败重试次数，大于零时生效
     */
    private Integer executorFailRetryCount = 0;
    /**
     * GLUE备注
     */
    private String glueRemark = "GLUE代码初始化";
    /**
     * GLUE源代码
     */
    private String glueSource;
}
