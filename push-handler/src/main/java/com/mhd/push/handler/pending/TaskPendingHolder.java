package com.mhd.push.handler.pending;

import com.mhd.push.handler.config.HandlerThreadPoolConfig;
import com.mhd.push.handler.utils.GroupIdMappingUtils;
import com.mhd.push.support.utils.ThreadPoolUtils;
import jakarta.annotation.Resource;
import org.dromara.dynamictp.core.DtpRegistry;
import org.dromara.dynamictp.core.executor.DtpExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * 存储 每种消息类型 与 TaskPending 的关系
 *
 * @author zhao-hao-dong
 */
@Component
public class TaskPendingHolder {
    /**
     * 获取得到所有的groupId
     */
    private static List<String> groupIds = GroupIdMappingUtils.getAllGroupIds();
    @Resource
    private ThreadPoolUtils threadPoolUtils;


    /**
     * 给每个渠道，每种消息类型初始化一个线程池
     */
    @PostConstruct
    public void init() {
        /**
         * example ThreadPoolName:austin.im.notice
         *
         * 可以通过apollo配置：dynamic-tp-apollo-dtp.yml  动态修改线程池的信息
         */
        for (String groupId : groupIds) {
            DtpExecutor executor = HandlerThreadPoolConfig.getExecutor(groupId);
            threadPoolUtils.register(executor);
        }
    }

    /**
     * 得到对应的线程池
     */
    public DtpExecutor route(String groupId) {
        return DtpRegistry.getDtpExecutor(HandlerThreadPoolConfig.PRE_FIX + groupId);
    }
}
