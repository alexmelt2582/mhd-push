package com.mhd.push.cron.xxl.constant;

/**
 * xxl-job常量信息
 *
 * @author zhao-hao-dong
 **/
public interface XxlJobConstant {
    /**
     * 任务信息接口路径
     */
    String LOGIN_URL = "/login";
    String ADD_URL = "/jobinfo/add";
    String UPDATE_URL = "/jobinfo/update";
    String REMOVE_URL = "/jobinfo/remove";
    String START_URL = "/jobinfo/start";
    String STOP_URL = "/jobinfo/stop";
    String TRIGGER_URL = "/jobinfo/trigger";

    /**
     * 执行器组接口路径
     */
    String PAGELIST_URL = "/jobinfo/pageList";
    String PAGELIST_LOG_URL = "/joblog/pageList";

    /**
     * 任务相关
     */
    String CLEAR_LOG_URL = "/joblog/clearLog";
    String KILL_LOG_URL = "/joblog/logKill";

    /**
     * 请求 xxl-job-admin 需要用到的 cookie
     */
    String COOKIE_PREFIX = "xxl_job_cookie_";
}
