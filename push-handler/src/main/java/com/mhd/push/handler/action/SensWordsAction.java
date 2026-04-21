package com.mhd.push.handler.action;

import com.mhd.push.common.domain.TaskInfo;
import com.mhd.push.common.dto.model.*;
import com.mhd.push.common.pipeline.BusinessProcess;
import com.mhd.push.common.pipeline.ProcessContext;
import com.mhd.push.handler.config.SensitiveWordsConfig;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 敏感词过滤
 *
 * @author zhao-hao-dong
 */
@Service
public class SensWordsAction implements BusinessProcess<TaskInfo> {
    @Resource
    private SensitiveWordsConfig sensitiveWordsConfig;

    @Override
    public void process(ProcessContext<TaskInfo> context) {
        switch (context.getProcessModel().getMsgType()) {
            // IM
            case 10:
                // 无文本内容，暂不做过滤处理
                break;
            // PUSH
            case 20:
                PushContentModel pushContentModel =
                        (PushContentModel) context.getProcessModel().getContentModel();
                pushContentModel.setContent(sensitiveWordsConfig.filter(pushContentModel.getContent()));
                break;
            // SMS
            case 30:
                SmsContentModel smsContentModel =
                        (SmsContentModel) context.getProcessModel().getContentModel();
                smsContentModel.setContent(sensitiveWordsConfig.filter(smsContentModel.getContent()));
                break;
            // EMAIL
            case 40:
                EmailContentModel emailContentModel =
                        (EmailContentModel) context.getProcessModel().getContentModel();
                emailContentModel.setContent(sensitiveWordsConfig.filter(emailContentModel.getContent()));
                break;
            // OFFICIAL_ACCOUNT
            case 50:
                // 无文本内容，暂不做过滤处理
                break;
            // MINI_PROGRAM
            case 60:
                // 无文本内容，暂不做过滤处理
                break;
            // ENTERPRISE_WE_CHAT
            case 70:
                EnterpriseWeChatContentModel enterpriseWeChatContentModel =
                        (EnterpriseWeChatContentModel) context.getProcessModel().getContentModel();
                enterpriseWeChatContentModel.setContent(sensitiveWordsConfig.filter(enterpriseWeChatContentModel.getContent()));
                break;
            // DING_DING_ROBOT
            case 80:
                DingDingRobotContentModel dingDingRobotContentModel =
                        (DingDingRobotContentModel) context.getProcessModel().getContentModel();
                dingDingRobotContentModel.setContent(sensitiveWordsConfig.filter(dingDingRobotContentModel.getContent()));
                break;
            // DING_DING_WORK_NOTICE
            case 90:
                DingDingWorkContentModel dingDingWorkContentModel =
                        (DingDingWorkContentModel) context.getProcessModel().getContentModel();
                dingDingWorkContentModel.setContent(sensitiveWordsConfig.filter(dingDingWorkContentModel.getContent()));
                break;
            // ENTERPRISE_WE_CHAT_ROBOT
            case 100:
                EnterpriseWeChatRobotContentModel enterpriseWeChatRobotContentModel =
                        (EnterpriseWeChatRobotContentModel) context.getProcessModel().getContentModel();
                enterpriseWeChatRobotContentModel.setContent(sensitiveWordsConfig.filter(enterpriseWeChatRobotContentModel.getContent()));
                break;
            // FEI_SHU_ROBOT
            case 110:
                FeiShuRobotContentModel feiShuRobotContentModel =
                        (FeiShuRobotContentModel) context.getProcessModel().getContentModel();
                feiShuRobotContentModel.setContent(sensitiveWordsConfig.filter(feiShuRobotContentModel.getContent()));
                break;
            // ALIPAY_MINI_PROGRAM
            case 120:
                // 无文本内容，暂不做过滤处理
                break;
            default:
                break;
        }
    }
}
