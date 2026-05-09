package com.mhd.push.common.web.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * 可重复读取请求体的过滤器。
 */
public class RepeatableFilter implements Filter {

    /**
     * 为 JSON 请求包装可重复读取的 request。
     *
     * @param request 原始请求
     * @param response 响应对象
     * @param chain 过滤器链
     * @throws IOException IO 异常
     * @throws ServletException Servlet 异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        ServletRequest requestWrapper = null;
        // 1. 仅对 JSON 请求包装，避免影响表单或文件上传等其他请求类型。
        if (request instanceof HttpServletRequest
                && StringUtils.startsWithIgnoreCase(request.getContentType(), MediaType.APPLICATION_JSON_VALUE)) {
            requestWrapper = new RepeatedlyRequestWrapper((HttpServletRequest) request, response);
        }
        // 2. 有包装对象时向后传递包装请求，否则保留原始请求。
        if (null == requestWrapper) {
            chain.doFilter(request, response);
        } else {
            chain.doFilter(requestWrapper, response);
        }
    }
}
