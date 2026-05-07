package com.mhd.push.publicapi.interceptor;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.map.MapUtil;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.mhd.push.publicapi.filter.RepeatedlyRequestWrapper;
import com.mhd.push.common.utils.JsonUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.io.BufferedReader;
import java.util.Map;

/**
 * Web 调用耗时与请求参数日志拦截器。
 */
@Slf4j
public class PlusWebInvokeTimeInterceptor implements HandlerInterceptor {
    private final TransmittableThreadLocal<StopWatch> invokeTimeTL = new TransmittableThreadLocal<>();

    /**
     * 在请求进入控制器前记录请求参数与开始时间。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param handler 处理器
     * @return true 表示继续执行
     * @throws Exception 处理异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String url = request.getMethod() + " " + request.getRequestURI();
        String domainName = request.getServerName();
        //log.info("域名信息：{}", domainName);
        // 1. 按请求类型打印参数，便于接口问题排查。
        if (isJsonRequest(request)) {
            String jsonParam = "";
            if (request instanceof RepeatedlyRequestWrapper) {
                BufferedReader reader = request.getReader();
                jsonParam = IoUtil.read(reader);
            }
            log.debug("[PLUS]开始请求 => URL[{}],参数类型[json],参数:[{}]", url, jsonParam);
        } else {
            Map<String, String[]> parameterMap = request.getParameterMap();
            if (MapUtil.isNotEmpty(parameterMap)) {
                String parameters = JsonUtils.toJsonString(parameterMap);
                log.debug("[PLUS]开始请求 => URL[{}],参数类型[param],参数:[{}]", url, parameters);
            } else {
                log.debug("[PLUS]开始请求 => URL[{}],无参数", url);
            }
        }
        // 2. 为本次请求创建独立计时器。
        StopWatch stopWatch = new StopWatch();
        invokeTimeTL.set(stopWatch);
        stopWatch.start();

        return true;
    }

    /**
     * 预留的 postHandle 钩子。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param handler 处理器
     * @param modelAndView 视图模型
     * @throws Exception 处理异常
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }

    /**
     * 在请求完成后输出结束日志并清理线程变量。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param handler 处理器
     * @param ex 异常对象
     * @throws Exception 处理异常
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        log.debug("结束请求 => URL[{}]", request.getRequestURI());
        invokeTimeTL.remove();
    }

    /**
     * 判断本次请求是否为 JSON。
     *
     * @param request 请求对象
     * @return true 表示 JSON 请求
     */
    private boolean isJsonRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType != null) {
            return StringUtils.startsWithIgnoreCase(contentType, MediaType.APPLICATION_JSON_VALUE);
        }
        return false;
    }
}
