package com.mhd.push.job.xxl.entity;//package com.mhd.push.cron.xxl.entity;
//
//import jakarta.annotation.Resource;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import java.net.HttpCookie;
//import java.util.*;
//
//
///**
// * Xxl job 远程接口
// *
// * @author zhao-hao-dong
// **/
//@Slf4j
//@Component
//public class XxlJobServiceApi {
//    @Value("${xxl.job.admin.username}")
//    private String userName;
//    @Value("${xxl.job.admin.password}")
//    private String password;
//    @Value("${xxl.job.admin.addresses}")
//    private String basicUrl;
//    @Resource
//    private XxlJobMapper xxlJobMapper;
//    public final Map<String, String> loginHeaderMap = new HashMap<>();
//
//    public void init() {
//        loginHeaderMap.put("Cookie", login());
//    }
//
//    public XxlJobInfo getXxlJobByJobDesc(String jobDesc) {
//        List<Map<String, Object>> xxlJobIdList = xxlJobMapper.selectXxlJobIdByJobDesc(jobDesc);
//        if (CollUtil.isEmpty(xxlJobIdList)) {
//            throw new BusinessException(BusinessEnum.QUERY_DATA_FAIL, "任务ID不存在");
//        }
//        Map<String, Object> map = xxlJobIdList.get(0);
//        int triggerStatus = Integer.parseInt(String.valueOf(map.get("trigger_status")));
//        int jobGroup = Integer.parseInt(String.valueOf(map.get("job_group")));
//        int jobId = Integer.parseInt(String.valueOf(map.get("id")));
//        String executorParam = String.valueOf(map.get("executor_param"));
//        String childJobid = String.valueOf(map.get("child_jobid"));
//        XxlJobInfo xxlJobInfo = new XxlJobInfo();
//        xxlJobInfo.setId(jobId);
//        xxlJobInfo.setJobGroup(jobGroup);
//        xxlJobInfo.setJobDesc(jobDesc);
//        xxlJobInfo.setExecutorParam(executorParam);
//        xxlJobInfo.setTriggerStatus(triggerStatus);
//        xxlJobInfo.setChildJobid(childJobid);
//        return xxlJobInfo;
//    }
//
//    public Integer getXxlJobGroup(String appName) {
//        return xxlJobMapper.selectGroupId(appName);
//    }
//
//    /**
//     * 登录接口
//     */
//    public String login() {
//        LinkedHashMap<String, Object> body = new LinkedHashMap<>();
//        body.put("userName", userName);
//        body.put("password", password);
//        List<HttpCookie> cookies = HttpUtils.postCookie(basicUrl + XxlJobServiceConstant.LOGIN_URL, null, body);
//        Optional<HttpCookie> cookieOpt = cookies.stream().filter(cookie -> cookie.getName().equals("XXL_JOB_LOGIN_IDENTITY")).findFirst();
//        if (cookieOpt.isEmpty()) {
//            throw BusinessException.of(XXL_JOB_LOGIN_FAIL);
//        }
//        return cookieOpt.get().getValue();
//    }
//
//    public XxlJobPageReturnDTO list(XxlJobQueryDTO xxlJobQueryDTO) {
//        validLogin();
//        Map<String, Object> paramMap = new HashMap<>();
//        paramMap.put("jobGroup", xxlJobQueryDTO.getJobGroup());
//        paramMap.put("triggerStatus", xxlJobQueryDTO.getTriggerStatus());
//        paramMap.put("jobDesc", xxlJobQueryDTO.getJobDesc());
//        paramMap.put("executorHandler", xxlJobQueryDTO.getExecutorHandler());
//        paramMap.put("author", xxlJobQueryDTO.getAuthor());
//        paramMap.put("start", xxlJobQueryDTO.getStart());
//        paramMap.put("length", xxlJobQueryDTO.getLength());
//        return handlePageResponse(HttpUtils.post(basicUrl + XxlJobServiceConstant.PAGELIST_URL, loginHeaderMap, paramMap));
//    }
//
//    /**
//     * 新增定时任务
//     * 返回值的content代表主键ID
//     */
//    public XxlJobReturnDTO add(XxlJobSaveDTO xxlJobSaveDTO) {
//        validLogin();
//        Map<String, Object> paramMap = new HashMap<>();
//        paramMap.put("jobGroup", xxlJobSaveDTO.getJobGroup());
//        paramMap.put("jobDesc", xxlJobSaveDTO.getJobDesc());
//        paramMap.put("author", xxlJobSaveDTO.getAuthor());
//        paramMap.put("alarmEmail", xxlJobSaveDTO.getAlarmEmail());
//        paramMap.put("scheduleType", xxlJobSaveDTO.getScheduleType());
//        paramMap.put("scheduleConf", xxlJobSaveDTO.getScheduleConf());
//        paramMap.put("cronGen_display", xxlJobSaveDTO.getCronGenDisplay());
//        paramMap.put("schedule_conf_CRON", xxlJobSaveDTO.getScheduleConfCRON());
//        paramMap.put("schedule_conf_FIX_RATE", xxlJobSaveDTO.getScheduleConfFIXRATE());
//        paramMap.put("schedule_conf_FIX_DELAY", xxlJobSaveDTO.getScheduleConfFIXDELAY());
//        paramMap.put("glueType", xxlJobSaveDTO.getGlueType());
//        paramMap.put("executorHandler", xxlJobSaveDTO.getExecutorHandler());
//        paramMap.put("executorParam", xxlJobSaveDTO.getExecutorParam());
//        paramMap.put("executorRouteStrategy", xxlJobSaveDTO.getExecutorRouteStrategy().getName());
//        paramMap.put("childJobId", xxlJobSaveDTO.getChildJobId());
//        paramMap.put("misfireStrategy", xxlJobSaveDTO.getMisfireStrategy().getName());
//        paramMap.put("executorBlockStrategy", xxlJobSaveDTO.getExecutorBlockStrategy().getName());
//        paramMap.put("executorTimeout", xxlJobSaveDTO.getExecutorTimeout());
//        paramMap.put("executorFailRetryCount", xxlJobSaveDTO.getExecutorFailRetryCount());
//        paramMap.put("glueRemark", xxlJobSaveDTO.getGlueRemark());
//        paramMap.put("glueSource", xxlJobSaveDTO.getGlueSource());
//        return handleResponse(HttpUtils.post(basicUrl + XxlJobServiceConstant.ADD_URL, loginHeaderMap, paramMap));
//    }
//
//    /**
//     * 编辑定时任务
//     */
//    public XxlJobReturnDTO update(XxlJobSaveDTO xxlJobSaveDTO) {
//        validLogin();
//        Map<String, Object> paramMap = new HashMap<>();
//        paramMap.put("id", xxlJobSaveDTO.getId());
//        paramMap.put("jobGroup", xxlJobSaveDTO.getJobGroup());
//        paramMap.put("jobDesc", xxlJobSaveDTO.getJobDesc());
//        paramMap.put("author", xxlJobSaveDTO.getAuthor());
//        paramMap.put("alarmEmail", xxlJobSaveDTO.getAlarmEmail());
//        paramMap.put("scheduleType", xxlJobSaveDTO.getScheduleType());
//        paramMap.put("scheduleConf", xxlJobSaveDTO.getScheduleConf() == null ? "" : xxlJobSaveDTO.getScheduleConf());
//        paramMap.put("cronGen_display", xxlJobSaveDTO.getCronGenDisplay());
//        paramMap.put("schedule_conf_CRON", xxlJobSaveDTO.getScheduleConfCRON());
//        paramMap.put("schedule_conf_FIX_RATE", xxlJobSaveDTO.getScheduleConfFIXRATE());
//        paramMap.put("schedule_conf_FIX_DELAY", xxlJobSaveDTO.getScheduleConfFIXDELAY());
//        paramMap.put("executorHandler", xxlJobSaveDTO.getExecutorHandler());
//        paramMap.put("executorParam", xxlJobSaveDTO.getExecutorParam());
//        paramMap.put("executorRouteStrategy", xxlJobSaveDTO.getExecutorRouteStrategy().getName());
//        paramMap.put("childJobId", xxlJobSaveDTO.getChildJobId() == null ? "" : xxlJobSaveDTO.getChildJobId());
//        paramMap.put("misfireStrategy", xxlJobSaveDTO.getMisfireStrategy().getName());
//        paramMap.put("executorBlockStrategy", xxlJobSaveDTO.getExecutorBlockStrategy().getName());
//        paramMap.put("executorTimeout", xxlJobSaveDTO.getExecutorTimeout());
//        paramMap.put("executorFailRetryCount", xxlJobSaveDTO.getExecutorFailRetryCount());
//        return handleResponse(HttpUtils.post(basicUrl + XxlJobServiceConstant.UPDATE_URL, loginHeaderMap, paramMap));
//    }
//
//
//    /**
//     * 启动定时任务
//     */
//    public XxlJobReturnDTO start(int jobId) {
//        validLogin();
//        Map<String, Object> paramMap = new HashMap<>();
//        paramMap.put("id", jobId);
//        return handleResponse(HttpUtils.post(basicUrl + XxlJobServiceConstant.START_URL, loginHeaderMap, paramMap));
//    }
//
//    /**
//     * 删除定时任务
//     */
//    public XxlJobReturnDTO remove(int jobId) {
//        validLogin();
//        Map<String, Object> paramMap = new HashMap<>();
//        paramMap.put("id", jobId);
//        return handleResponse(HttpUtils.post(basicUrl + XxlJobServiceConstant.REMOVE_URL, loginHeaderMap, paramMap));
//    }
//
//    /**
//     * 暂停定时任务
//     */
//    public XxlJobReturnDTO stop(int jobId) {
//        validLogin();
//        Map<String, Object> paramMap = new HashMap<>();
//        paramMap.put("id", jobId);
//        return handleResponse(HttpUtils.post(basicUrl + XxlJobServiceConstant.STOP_URL, loginHeaderMap, paramMap));
//    }
//
//    public XxlJobReturnDTO trigger(int jobId, String executorParam, String addressList) {
//        validLogin();
//        Map<String, Object> paramMap = new HashMap<>();
//        paramMap.put("id", jobId);
//        paramMap.put("executorParam", executorParam);
//        paramMap.put("addressList", addressList);
//        return handleResponse(HttpUtils.post(basicUrl + XxlJobServiceConstant.TRIGGER_URL, loginHeaderMap, paramMap));
//    }
//
//    public XxlJobReturnDTO killLog(int id) {
//        validLogin();
//        Map<String, Object> paramMap = new HashMap<>();
//        paramMap.put("id", id);
//        return handleResponse(HttpUtils.post(basicUrl + XxlJobServiceConstant.KILL_LOG_URL, loginHeaderMap, paramMap));
//    }
//
//    public XxlJobReturnDTO clearLog(int jobGroup, int jobId) {
//        validLogin();
//        Map<String, Object> paramMap = new HashMap<>();
//        paramMap.put("jobGroup", jobGroup);
//        paramMap.put("jobId", jobId);
//        paramMap.put("type", 9);
//        return handleResponse(HttpUtils.post(basicUrl + XxlJobServiceConstant.CLEAR_LOG_URL, loginHeaderMap, paramMap));
//    }
//
//    public List<Integer> meGetRunningLogs(List<XxlJobInfo> xxlJobInfoList, int group) {
//        if (CollUtil.isEmpty(xxlJobInfoList)) {
//            return Collections.emptyList();
//        }
//        List<Integer> idList = new ArrayList<>();
//        for (XxlJobInfo xxlJobInfo : xxlJobInfoList) {
//            List<Map<String, Object>> mapList = xxlJobMapper.selectXxlJobLog(group, xxlJobInfo.getId(), xxlJobInfo.getExecutorParam(), 3);
//            if (CollUtil.isEmpty(mapList)) {
//                continue;
//            }
//            for (Map<String, Object> map : mapList) {
//                // 获取id值并添加到列表中
//                if (map.containsKey("id")) {
//                    idList.add(Integer.parseInt(String.valueOf(map.get("id"))));
//                }
//            }
//        }
//        return idList;
//    }
//
//    public void meKillJob(List<XxlJobInfo> xxlJobInfoList, int group) {
//        init();
//        if (CollUtil.isEmpty(xxlJobInfoList)) {
//            return;
//        }
//        List<Integer> ids = meGetRunningLogs(xxlJobInfoList, group);
//        if (!ids.isEmpty()) {
//            ids.forEach(this::killLog);
//        }
//    }
//
//    private void validLogin() {
//        if (loginHeaderMap.isEmpty()) {
//            throw ValidationException.of(XXL_JOB_NOT_LOGIN);
//        }
//    }
//
//    private XxlJobReturnDTO handleResponse(String response) {
//        log.debug("XxlJobServiceApi handleResponse response: {}", response);
//        XxlJobReturnDTO xxlJobReturnDTO = JSON.parseObject(response, XxlJobReturnDTO.class);
//        if (xxlJobReturnDTO.getCode() != 200) {
//            throw BusinessException.of(XXL_JOB_REQUEST_FAIL, xxlJobReturnDTO.getMsg());
//        }
//        return xxlJobReturnDTO;
//    }
//
//    private XxlJobPageReturnDTO handlePageResponse(String response) {
//        log.debug("XxlJobServiceApi handlePageResponse response: {}", response);
//        return JSON.parseObject(response, XxlJobPageReturnDTO.class);
//    }
//}
