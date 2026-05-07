package com.mhd.push.engine.handler.impl;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson2.JSON;
import com.google.common.base.Throwables;
import com.mhd.push.common.log.LogRecord;
import com.mhd.push.domain.model.task.RecallTaskInfo;
import com.mhd.push.domain.model.task.TaskInfo;
import com.mhd.push.common.dto.model.OfficialAccountsContentModel;
import com.mhd.push.common.enums.ChannelType;
import com.mhd.push.common.enums.SendTypeEnum;
import com.mhd.push.engine.handler.BaseHandler;
import com.mhd.push.engine.utils.AccountUtils;
import com.mhd.push.infra.utils.LogUtils;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateData;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author zyg
 * 微信服务号推送处理
 * https://developers.weixin.qq.com/doc/offiaccount/Subscription_Messages/api.html
 */
@Component
@Slf4j
public class OfficialAccountHandler extends BaseHandler {

    @Autowired
    private AccountUtils accountUtils;
    @Autowired
    private LogUtils logUtils;

    public OfficialAccountHandler() {
        channelCode = ChannelType.OFFICIAL_ACCOUNT.getCode();
    }

    @Override
    public boolean handler(TaskInfo taskInfo) {
        try {
            OfficialAccountsContentModel contentModel = (OfficialAccountsContentModel) taskInfo.getContentModel();
            WxMpService wxMpService = accountUtils.getAccountById(taskInfo.getSendAccount(), WxMpService.class);

            WxMpTemplateMessage message = assembleReq(taskInfo.getReceiver(), contentModel);
            wxMpService.getTemplateMsgService().sendTemplateMsg(message);

            return true;
        } catch (WxErrorException e) {
            LogRecord logRecord = LogRecord.build(SendTypeEnum.SEND, taskInfo, e.getError().getErrorCode(), e.getError().getErrorMsg());
            logUtils.print(logRecord);
        } catch (Exception e) {
            log.error("OfficialAccountHandler#handler fail:{},params:{}", Throwables.getStackTraceAsString(e), JSON.toJSONString(taskInfo));
        }
        return false;
    }

    /**
     * 组装发送模板信息参数
     *
     * @param receiver     接收信息者
     * @param contentModel 消息下发参数
     */
    private WxMpTemplateMessage assembleReq(Set<String> receiver, OfficialAccountsContentModel contentModel) {
        return WxMpTemplateMessage.builder()
                .toUser(CollUtil.getFirst(receiver.iterator()))
                .templateId(contentModel.getTemplateId())
                .url(contentModel.getUrl())
                .data(getWxMpTemplateData(contentModel.getOfficialAccountParam()))
                .miniProgram(new WxMpTemplateMessage.MiniProgram(contentModel.getMiniProgramId(), contentModel.getPath(), false))
                .build();
    }

    /**
     * 构建模板消息参数
     *
     * @param data 模板参数
     * @return
     */
    private List<WxMpTemplateData> getWxMpTemplateData(Map<String, String> data) {
        List<WxMpTemplateData> templateDataList = new ArrayList<>(data.size());
        data.forEach((k, v) -> templateDataList.add(new WxMpTemplateData(k, v)));
        return templateDataList;
    }


    /**
     * 微信服务号消息 不支持撤回
     * https://developers.weixin.qq.com/doc/offiaccount/Subscription_Messages/api.html
     *
     * @param recallTaskInfo
     */
    @Override
    public void recall(RecallTaskInfo recallTaskInfo) {

    }
}

