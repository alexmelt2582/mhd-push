package com.mhd.push.handler.handler.impl;

import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSON;
import com.google.common.base.Throwables;
import com.mhd.push.common.domain.AnchorInfo;
import com.mhd.push.common.domain.RecallTaskInfo;
import com.mhd.push.common.domain.TaskInfo;
import com.mhd.push.common.dto.account.EnterpriseWeChatRobotAccount;
import com.mhd.push.common.dto.model.EnterpriseWeChatRobotContentModel;
import com.mhd.push.common.enums.ChannelType;
import com.mhd.push.common.enums.SendMessageType;
import com.mhd.push.handler.domain.wechat.robot.EnterpriseWeChatRobotParam;
import com.mhd.push.handler.domain.wechat.robot.EnterpriseWeChatRootResult;
import com.mhd.push.handler.handler.BaseHandler;
import com.mhd.push.handler.utils.AccountUtils;
import com.mhd.push.support.utils.LogUtils;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxCpErrorMsgEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 企业微信群机器人 消息处理器
 * https://developer.work.weixin.qq.com/document/path/91770
 *
 * @author 3y
 */
@Slf4j
@Service
public class EnterpriseWeChatRobotHandler extends BaseHandler {

    @Autowired
    private AccountUtils accountUtils;
    @Autowired
    private LogUtils logUtils;

    public EnterpriseWeChatRobotHandler() {
        channelCode = ChannelType.ENTERPRISE_WE_CHAT_ROBOT.getCode();
    }

    @Override
    public boolean handler(TaskInfo taskInfo) {
        try {
            EnterpriseWeChatRobotAccount account = accountUtils.getAccountById(taskInfo.getSendAccount(), EnterpriseWeChatRobotAccount.class);
            EnterpriseWeChatRobotParam enterpriseWeChatRobotParam = assembleParam(taskInfo);
            HttpResponse response = HttpRequest.post(account.getWebhook()).header(Header.CONTENT_TYPE.getValue(), ContentType.JSON.getValue())
                    .body(JSON.toJSONString(enterpriseWeChatRobotParam))
                    .timeout(2000)
                    .execute();
            if (response.getStatus() == 429) {
                recordExternalRateLimitBackoff(taskInfo, 1000L);
                log.warn("EnterpriseWeChatRobotHandler#handler throttled, params:{}", JSON.toJSONString(taskInfo));
                return false;
            }
            String result = response.body();
            EnterpriseWeChatRootResult weChatRootResult = JSON.parseObject(result, EnterpriseWeChatRootResult.class);
            if (Integer.valueOf(WxCpErrorMsgEnum.CODE_0.getCode()).equals(weChatRootResult.getErrcode())) {
                return true;
            }
            logUtils.print(AnchorInfo.builder().bizId(taskInfo.getBizId()).messageId(taskInfo.getMessageId()).businessId(taskInfo.getBusinessId())
                    .ids(taskInfo.getReceiver()).state(weChatRootResult.getErrcode()).build());
        } catch (Exception e) {
            log.error("EnterpriseWeChatRobotHandler#handler fail!e:{},params:{}", Throwables.getStackTraceAsString(e), JSON.toJSONString(taskInfo));
        }
        return false;
    }

    private EnterpriseWeChatRobotParam assembleParam(TaskInfo taskInfo) {
        EnterpriseWeChatRobotContentModel contentModel = (EnterpriseWeChatRobotContentModel) taskInfo.getContentModel();
        EnterpriseWeChatRobotParam param = EnterpriseWeChatRobotParam.builder()
                .msgType(SendMessageType.getEnterpriseWeChatRobotTypeByCode(contentModel.getSendType())).build();

        if (SendMessageType.TEXT.getCode().equals(contentModel.getSendType())) {
            param.setText(EnterpriseWeChatRobotParam.TextDTO.builder().content(contentModel.getContent()).build());
        }
        if (SendMessageType.MARKDOWN.getCode().equals(contentModel.getSendType())) {
            param.setMarkdown(EnterpriseWeChatRobotParam.MarkdownDTO.builder().content(contentModel.getContent()).build());
        }
        if (SendMessageType.IMAGE.getCode().equals(contentModel.getSendType())) {
            param.setImage(EnterpriseWeChatRobotParam.ImageDTO.builder().base64(contentModel.getBase64()).md5(contentModel.getMd5()).build());
        }
        if (SendMessageType.FILE.getCode().equals(contentModel.getSendType())) {
            param.setFile(EnterpriseWeChatRobotParam.FileDTO.builder().mediaId(contentModel.getMediaId()).build());
        }
        if (SendMessageType.NEWS.getCode().equals(contentModel.getSendType())) {
            List<EnterpriseWeChatRobotParam.NewsDTO.ArticlesDTO> articlesDtoS = JSON.parseArray(contentModel.getArticles(), EnterpriseWeChatRobotParam.NewsDTO.ArticlesDTO.class);
            param.setNews(EnterpriseWeChatRobotParam.NewsDTO.builder().articles(articlesDtoS).build());
        }
        if (SendMessageType.TEMPLATE_CARD.getCode().equals(contentModel.getSendType())) {
            //
        }
        return param;
    }


    /**
     * 企业微信群机器人 不支持撤回消息
     * https://developer.work.weixin.qq.com/document/path/91770
     *
     * @param recallTaskInfo
     */
    @Override
    public void recall(RecallTaskInfo recallTaskInfo) {

    }
}

