package com.mhd.push.engine.pending;

import com.mhd.push.common.constant.ThreadPoolConstant;
import com.mhd.push.engine.utils.GroupIdMappingUtils;
import com.mhd.push.infra.config.HandlerThreadPoolConfig;
import com.mhd.push.infra.utils.ThreadPoolUtils;
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
        * 初始化每个分组的分发线程池和业务线程池。
     */
    @PostConstruct
    public void init() {
        /**
         * example ThreadPoolName:austin.im.notice
         *
         * 可以通过apollo配置：dynamic-tp-apollo-dtp.yml  动态修改线程池的信息
         */
        for (String groupId : groupIds) {
            DtpExecutor dispatchExecutor = HandlerThreadPoolConfig.getDispatchExecutor(groupId);
            DtpExecutor executor = HandlerThreadPoolConfig.getExecutor(groupId);
            threadPoolUtils.register(dispatchExecutor);
            threadPoolUtils.register(executor);
            for (int i = 0; i < ThreadPoolConstant.ORDERLY_EXECUTOR_STRIPES; i++) {
                threadPoolUtils.register(HandlerThreadPoolConfig.getOrderlyExecutor(groupId, i));
            }
        }
    }

    /**
     * 提交任务到分发线程池。
     *
     * 分发线程池只做一件事：把真正的业务任务转交给渠道线程池，
     * 这样即便业务池发生反压，也优先阻塞分发层，而不是直接阻塞 MQ 消费线程。
     */
    public void submit(String groupId, Task task) {
        routeDispatch(groupId).execute(() -> route(groupId).execute(task));
    }

    /**
     * 按顺序键分片后的串行执行。
     */
    public void submitOrderly(String groupId, String orderKey, Task task) {
        routeOrderly(groupId, stripeIndex(orderKey)).execute(task);
    }

    /**
     * 根据分组获取业务线程池。
     */
    public DtpExecutor route(String groupId) {
        return DtpRegistry.getDtpExecutor(HandlerThreadPoolConfig.PRE_FIX + groupId);
    }

    /**
     * 根据分组获取分发线程池。
     */
    public DtpExecutor routeDispatch(String groupId) {
        return DtpRegistry.getDtpExecutor(HandlerThreadPoolConfig.DISPATCH_PRE_FIX + groupId);
    }

    public DtpExecutor routeOrderly(String groupId, int index) {
        return DtpRegistry.getDtpExecutor(HandlerThreadPoolConfig.ORDERLY_PRE_FIX + groupId + "." + index);
    }

    private int stripeIndex(String orderKey) {
        int stripeCount = ThreadPoolConstant.ORDERLY_EXECUTOR_STRIPES;
        if (orderKey == null || orderKey.isBlank()) {
            return 0;
        }
        return Math.abs(orderKey.hashCode()) % stripeCount;
    }
}
