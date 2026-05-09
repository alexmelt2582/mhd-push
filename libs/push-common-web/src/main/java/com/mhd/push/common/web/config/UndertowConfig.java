package com.mhd.push.common.web.config;

import io.undertow.server.DefaultByteBufferPool;
import io.undertow.server.handlers.DisallowedMethodsHandler;
import io.undertow.util.HttpString;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;

/**
 * Undertow 自定义配置
 *
 * @author zhao-hao-dong
 */
@AutoConfiguration
public class UndertowConfig implements WebServerFactoryCustomizer<UndertowServletWebServerFactory> {
    @Override
    public void customize(UndertowServletWebServerFactory factory) {
        factory.addDeploymentInfoCustomizers(deploymentInfo -> {
            // 配置 WebSocket 部署信息，设置 WebSocket 使用的缓冲区池
            WebSocketDeploymentInfo webSocketDeploymentInfo = new WebSocketDeploymentInfo();
            webSocketDeploymentInfo.setBuffers(new DefaultByteBufferPool(true, 1024));
            deploymentInfo.addServletContextAttribute("io.undertow.websockets.jsr.WebSocketDeploymentInfo", webSocketDeploymentInfo);

            // 配置禁止某些不安全的 HTTP 方法（如 CONNECT、TRACE、TRACK）
            deploymentInfo.addInitialHandlerChainWrapper(handler -> {
                // 禁止三个方法 CONNECT/TRACE/TRACK 也是不安全的 避免爬虫骚扰
                HttpString[] disallowedHttpMethods = {
                        HttpString.tryFromString("CONNECT"),
                        HttpString.tryFromString("TRACE"),
                        HttpString.tryFromString("TRACK")
                };
                // 使用 DisallowedMethodsHandler 拦截并拒绝这些方法的请求
                return new DisallowedMethodsHandler(handler, disallowedHttpMethods);
            });

            // DisallowedMethodsHandler 与 SecurityConstraint 目的一致，DisallowedMethodsHandler先执行，会返回405，SecurityConstraint会返回403
            //SecurityConstraint constraint = new SecurityConstraint()
            //        .addWebResourceCollection(new WebResourceCollection()
            //                .addUrlPattern("/*")
            //                .addHttpMethod(HttpMethod.TRACE.name()))
            //        .setEmptyRoleSemantic(SecurityInfo.EmptyRoleSemantic.DENY);
            //deploymentInfo.addSecurityConstraint(constraint);
        });
    }
}
