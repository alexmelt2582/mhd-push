package com.mhd.push.infra.config;

import com.mhd.push.common.constant.ThreadPoolConstant;
import org.dromara.dynamictp.common.em.QueueTypeEnum;
import org.dromara.dynamictp.common.em.RejectedTypeEnum;
import org.dromara.dynamictp.core.executor.DtpExecutor;
import org.dromara.dynamictp.core.support.ThreadPoolBuilder;

import java.util.concurrent.TimeUnit;

/**
 * @author zhao-hao-dong
 */
public class HandlerThreadPoolConfig {
    public static final String PRE_FIX = "mhd.";
    public static final String DISPATCH_PRE_FIX = "mhd.dispatch.";
    public static final String ORDERLY_PRE_FIX = "mhd.orderly.";

    private HandlerThreadPoolConfig() {

    }

    /**
     * 业务：处理某个渠道的某种类型消息的线程池
     * 配置：不丢弃消息，核心线程数不会随着keepAliveTime而减少(不会被回收)
     * 动态线程池且被Spring管理：true
     */
    public static DtpExecutor getExecutor(String groupId) {
        return ThreadPoolBuilder.newBuilder()
                .threadPoolName(PRE_FIX + groupId)
                .corePoolSize(ThreadPoolConstant.COMMON_CORE_POOL_SIZE)
                .maximumPoolSize(ThreadPoolConstant.COMMON_MAX_POOL_SIZE)
                .keepAliveTime(ThreadPoolConstant.COMMON_KEEP_LIVE_TIME)
                .timeUnit(TimeUnit.SECONDS)
                .rejectedExecutionHandler(RejectedTypeEnum.CALLER_RUNS_POLICY.getName())
                .allowCoreThreadTimeOut(false)
                .workQueue(QueueTypeEnum.VARIABLE_LINKED_BLOCKING_QUEUE.getName(), ThreadPoolConstant.COMMON_QUEUE_SIZE, false)
                .buildDynamic();
    }

    /**
     * 创建提交线程池。
     *
     * 这个线程池只负责把任务从 MQ 消费线程转交到业务线程池，
     * 从而把 `CallerRunsPolicy` 的反压先拦在分发层，而不是直接压到消费线程。
     */
    public static DtpExecutor getDispatchExecutor(String groupId) {
        return ThreadPoolBuilder.newBuilder()
                .threadPoolName(DISPATCH_PRE_FIX + groupId)
                .corePoolSize(ThreadPoolConstant.SINGLE_CORE_POOL_SIZE)
                .maximumPoolSize(ThreadPoolConstant.SINGLE_MAX_POOL_SIZE)
                .keepAliveTime(ThreadPoolConstant.SMALL_KEEP_LIVE_TIME)
                .timeUnit(TimeUnit.SECONDS)
                .rejectedExecutionHandler(RejectedTypeEnum.CALLER_RUNS_POLICY.getName())
                .allowCoreThreadTimeOut(false)
                .workQueue(QueueTypeEnum.VARIABLE_LINKED_BLOCKING_QUEUE.getName(), ThreadPoolConstant.BIG_QUEUE_SIZE, false)
                .buildDynamic();
    }

    /**
     * 有序消息执行器：同一条业务顺序键落在同一个单线程执行器上，确保本地执行顺序不被打散。
     */
    public static DtpExecutor getOrderlyExecutor(String groupId, int index) {
        return ThreadPoolBuilder.newBuilder()
                .threadPoolName(ORDERLY_PRE_FIX + groupId + "." + index)
                .corePoolSize(ThreadPoolConstant.SINGLE_CORE_POOL_SIZE)
                .maximumPoolSize(ThreadPoolConstant.SINGLE_MAX_POOL_SIZE)
                .keepAliveTime(ThreadPoolConstant.SMALL_KEEP_LIVE_TIME)
                .timeUnit(TimeUnit.SECONDS)
                .rejectedExecutionHandler(RejectedTypeEnum.CALLER_RUNS_POLICY.getName())
                .allowCoreThreadTimeOut(false)
                .workQueue(QueueTypeEnum.VARIABLE_LINKED_BLOCKING_QUEUE.getName(), ThreadPoolConstant.BIG_QUEUE_SIZE, false)
                .buildDynamic();
    }
}
