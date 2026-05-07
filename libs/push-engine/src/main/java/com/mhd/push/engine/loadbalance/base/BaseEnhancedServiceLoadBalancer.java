package com.mhd.push.engine.loadbalance.base;

import com.mhd.push.engine.domain.channel.sms.MessageTypeSmsConfig;
import com.mhd.push.engine.loadbalance.ServiceLoadBalancer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public abstract class BaseEnhancedServiceLoadBalancer implements ServiceLoadBalancer<MessageTypeSmsConfig> {

    /**
     * 根据权重重新生成服务元数据列表，权重越高的元数据，会在最终的列表中出现的次数越多
     * 例如，权重为1，最终出现1次，权重为2，最终出现2次，权重为3，最终出现3次，依此类推...
     */
    protected List<MessageTypeSmsConfig> getWeightMessageTypeSmsConfigList(List<MessageTypeSmsConfig> servers) {
        List<MessageTypeSmsConfig> list = new ArrayList<>();
        servers.forEach((server) -> {
            IntStream.range(0, server.getWeights()).forEach((i) -> {
                list.add(server);
            });
        });
        return list;
    }
}
