package com.mhd.push.common.pipeline;

/**
 * 责任链处理器
 *
 * @author zhao-hao-dong

 **/
public interface BusinessProcess<T extends ProcessModel> {
    /**
     * 处理逻辑
     */
    void process(ProcessContext<T> context);
}
