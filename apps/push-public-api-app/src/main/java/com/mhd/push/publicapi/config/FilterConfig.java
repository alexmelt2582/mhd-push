package com.mhd.push.publicapi.config;

import com.mhd.push.publicapi.filter.RepeatableFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Filter 配置类。
 */
@Configuration
public class FilterConfig {

    /**
     * 注册请求体可重复读取过滤器。
     *
     * @return 过滤器注册对象
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Bean
    public FilterRegistrationBean someFilterRegistration() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        // 1. 全局注册 repeatable filter，支持日志与验签类组件重复读取请求体。
        registration.setFilter(new RepeatableFilter());
        registration.addUrlPatterns("/*");
        registration.setName("repeatableFilter");
        registration.setOrder(FilterRegistrationBean.LOWEST_PRECEDENCE);
        return registration;
    }
}
