package com.mhd.push.engine.action;

import com.mhd.push.common.dto.model.*;
import com.mhd.push.common.enums.MsgPushState;
import com.mhd.push.common.enums.SendTypeEnum;
import com.mhd.push.common.log.LogRecord;
import com.mhd.push.common.pipeline.BusinessProcess;
import com.mhd.push.common.pipeline.ProcessContext;
import com.mhd.push.domain.model.task.TaskInfo;
import com.mhd.push.engine.config.SensitiveWordsConfig;
import com.mhd.push.infra.utils.LogUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 敏感词过滤
 *
 * @author zhao-hao-dong
 */
@Service
@Slf4j
public class SensWordsAction implements BusinessProcess<TaskInfo> {
    @Resource
    private SensitiveWordsConfig sensitiveWordsConfig;
    @Autowired
    private LogUtils logUtils;

    @Override
    public void process(ProcessContext<TaskInfo> context) {
        TaskInfo taskInfo = context.getProcessModel();
        try {
            switch (context.getProcessModel().getSendChannel()) {
                // IM
                case 10:
                    // 无文本内容，暂不做过滤处理
                    break;
                // PUSH
                case 20:
                    PushContentModel pushContentModel =
                            (PushContentModel) taskInfo.getContentModel();
                    pushContentModel.setContent(sensitiveWordsConfig.filter(pushContentModel.getContent()));
                    break;
                // SMS
                case 30:
                    SmsContentModel smsContentModel =
                            (SmsContentModel) taskInfo.getContentModel();
                    smsContentModel.setContent(sensitiveWordsConfig.filter(smsContentModel.getContent()));
                    break;
                // EMAIL
                case 40:
                    EmailContentModel emailContentModel =
                            (EmailContentModel) taskInfo.getContentModel();
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
                            (EnterpriseWeChatContentModel) taskInfo.getContentModel();
                    enterpriseWeChatContentModel.setContent(sensitiveWordsConfig.filter(enterpriseWeChatContentModel.getContent()));
                    break;
                // DING_DING_ROBOT
                case 80:
                    DingDingRobotContentModel dingDingRobotContentModel =
                            (DingDingRobotContentModel) taskInfo.getContentModel();
                    dingDingRobotContentModel.setContent(sensitiveWordsConfig.filter(dingDingRobotContentModel.getContent()));
                    break;
                // DING_DING_WORK_NOTICE
                case 90:
                    DingDingWorkContentModel dingDingWorkContentModel =
                            (DingDingWorkContentModel) taskInfo.getContentModel();
                    dingDingWorkContentModel.setContent(sensitiveWordsConfig.filter(dingDingWorkContentModel.getContent()));
                    break;
                // ENTERPRISE_WE_CHAT_ROBOT
                case 100:
                    EnterpriseWeChatRobotContentModel enterpriseWeChatRobotContentModel =
                            (EnterpriseWeChatRobotContentModel) taskInfo.getContentModel();
                    enterpriseWeChatRobotContentModel.setContent(sensitiveWordsConfig.filter(enterpriseWeChatRobotContentModel.getContent()));
                    break;
                // FEI_SHU_ROBOT
                case 110:
                    FeiShuRobotContentModel feiShuRobotContentModel =
                            (FeiShuRobotContentModel) taskInfo.getContentModel();
                    feiShuRobotContentModel.setContent(sensitiveWordsConfig.filter(feiShuRobotContentModel.getContent()));
                    break;
                // ALIPAY_MINI_PROGRAM
                case 120:
                    // 无文本内容，暂不做过滤处理
                    break;
                default:
                    break;
            }
            LogRecord logRecord = LogRecord.build(SendTypeEnum.SEND, taskInfo, MsgPushState.SENSITIVE_MODULE_SUCCESS);
            logUtils.print(logRecord);
        } catch (Exception e) {
            LogRecord logRecord = LogRecord.build(SendTypeEnum.SEND, taskInfo, MsgPushState.SENSITIVE_MODULE_FAIL);
            logUtils.printError(logRecord);
            throw new RuntimeException(e);
        }
    }
}
