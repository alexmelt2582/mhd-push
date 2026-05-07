package com.mhd.push.engine.loadbalance.annotations;

import com.mhd.push.common.enums.LoadBalancerStrategy;
import org.springframework.stereotype.Service;

import java.lang.annotation.*;

/**
 * 负载均衡策略
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Service
public @interface LoadBalancer {

    String loadbalancer() default LoadBalancerStrategy.SERVICE_LOAD_BALANCER_RANDOM_WEIGHT_ENHANCED;
}
