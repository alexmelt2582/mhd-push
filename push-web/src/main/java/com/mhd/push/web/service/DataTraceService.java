package com.mhd.push.web.service;

import com.mhd.push.web.domain.dto.DataParam;
import com.mhd.push.web.domain.vo.EchartsVo;
import com.mhd.push.web.domain.vo.SmsTimeLineVo;
import com.mhd.push.web.domain.vo.UserTimeLineVo;

import java.util.Map;

/**
 * 数据链路追踪获取接口
 *
 * @author zhao-hao-dong
 */
public interface DataTraceService {
    /**
     * 获取全链路追踪 消息自身维度信息
     *
     * @param messageId 消息
     * @return
     */
    UserTimeLineVo getTraceMessageInfo(String messageId);

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
