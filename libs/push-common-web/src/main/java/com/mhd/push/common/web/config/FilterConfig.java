package com.mhd.push.common.web.config;

import com.mhd.push.common.web.filter.RepeatableFilter;
import com.mhd.push.common.web.filter.XssFilter;
import com.mhd.push.common.web.properties.XssProperties;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * Filter 配置类
 *
 * @author zhao-hao-dong
 */
@AutoConfiguration
@EnableConfigurationProperties(XssProperties.class)
public class FilterConfig {
    @Bean
    @ConditionalOnProperty(value = "xss.enabled", havingValue = "true")
    @FilterRegistration(
            name = "xssFilter",
            urlPatterns = "/*",
            order = FilterRegistrationBean.HIGHEST_PRECEDENCE + 1,
            dispatcherTypes = DispatcherType.REQUEST
    )
    public XssFilter xssFilter() {
        return new XssFilter();
    }

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
