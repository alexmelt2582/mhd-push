package com.mhd.push.engine.receipt.stater.impl;//package com.mhd.push.handler.receipt.stater.impl;
//
//import com.mhd.push.common.constant.CommonConstant;
//import com.mhd.push.common.enums.ChannelType;
//import com.mhd.push.handler.handler.impl.DingDingWorkNoticeHandler;
//import com.mhd.push.handler.receipt.stater.ReceiptMessageStater;
//import com.mhd.push.support.domain.entity.ChannelAccount;
//import org.springframework.beans.factory.annotation.Autowired;
//
//import java.util.List;
//
/// **
// * 拉取 钉钉工作消息的回执 内容 【未完成】
// *
// * @author 3y
// */
//public class DingDingWorkReceiptStaterImpl implements ReceiptMessageStater {
//
//    @Autowired
//    private DingDingWorkNoticeHandler workNoticeHandler;
//
//    @Autowired
//    private ChannelAccountDao channelAccountDao;
//
//    @Override
//    public void start() {
//        List<ChannelAccount> accountList = channelAccountDao.findAllByIsDeletedEqualsAndSendChannelEquals(CommonConstant.FALSE, ChannelType.DING_DING_WORK_NOTICE.getCode());
//        for (ChannelAccount channelAccount : accountList) {
//            workNoticeHandler.pull(channelAccount.getId());
//        }
//    }
//}
