package com.mhd.push.adminapi.service;

import com.mhd.push.adminapi.domain.dto.DataParam;
import com.mhd.push.adminapi.domain.vo.SmsTimeLineVo;
import com.mhd.push.adminapi.domain.vo.UserTimeLineVo;

import java.util.Map;

/**
 * 数据链路追踪获取接口
 *
 * @author zhao-hao-dong
 */
public interface DataTraceService {

    /**
     * 根据唯一shortCode获取全链路追踪
     *
     * @param shortCode 唯一标识
     * @return 消息信息
     */
    UserTimeLineVo getTraceByShortCode(String shortCode);

    /**
     * 获取全链路追踪 用户维度信息
     *
     * @param receiver 接收者
     * @return
     */
    UserTimeLineVo getTraceUserInfo(String receiver);

    /**
     * 获取全链路追踪 消息模板维度信息
     *
     * @param businessId 业务ID（如果传入消息模板ID，则生成当天的业务ID）
     * @return
     */
    Map<Object, Object> getTraceMessageTemplateInfo(String businessId);

    /**
     * 获取短信下发记录
     *
     * @param dataParam
     * @return
     */
    SmsTimeLineVo getTraceSmsInfo(DataParam dataParam);
}
