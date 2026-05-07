package com.mhd.push.infra.config.service.impl;

import cn.hutool.setting.dialect.Props;
import com.mhd.push.infra.config.service.ConfigService;
import com.mhd.push.infra.utils.NacosUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * 统一配置服务
 * 策略：优先 Nacos -> 其次 Apollo -> 最后 本地文件
 *
 * @author zhao-hao-dong
 */
@Service
@Slf4j
public class ConfigServiceImpl implements ConfigService {
    @Value("${mhd.nacos.enable:false}")
    private Boolean enableNacos;
    @Autowired(required = false) // 允许注入失败（如果没有 Nacos 环境）
    private NacosUtils nacosUtils;
    // 可选：Apollo 开关
    @Value("${apollo.bootstrap.enabled:false}")
    private Boolean enableApollo;
    @Value("${apollo.bootstrap.namespaces:application}")
    private String namespaces;
    /**
     * 本地配置文件路径，可通过环境变量或启动参数覆盖
     */
    @Value("${mhd.config.local.path:local.properties}")
    private String localConfigPath;
    /**
     * 本地配置加载器 (建议单例或缓存)
     */
    private volatile Props localProps;

    @PostConstruct
    public void init() {
        // 初始化本地配置，避免每次调用都重新加载（视 Props 类的具体实现而定，如果是内存映射则无所谓）
        try {
            this.localProps = new Props(localConfigPath, StandardCharsets.UTF_8);
            log.info("Local config loaded from: {}", localConfigPath);
        } catch (Exception e) {
            log.error("Failed to load local config file: {}", localConfigPath, e);
            // 兜底策略：创建一个空的 Props 或者抛出异常阻止启动
            this.localProps = new Props();
        }
    }

    /**
     * 获取配置属性
     * 优先级：Nacos > Apollo > Local
     */
    @Override
    public String getProperty(String key, String defaultValue) {
        String value = null;

        // 1. 尝试从 Nacos 获取
        if (Boolean.TRUE.equals(enableNacos) && nacosUtils != null) {
            try {
                value = nacosUtils.getProperty(key, null);
                if (value != null) return value;
            } catch (Exception e) {
                log.debug("Nacos get property failed: {}", e.getMessage());
            }
        }

        // 2. TODO 尝试从 Apollo 获取 (如果开启了且 Nacos 没找到)
        if (Boolean.TRUE.equals(enableApollo)) {
            //try {
            //    // 简单起见取第一个命名空间，生产环境可根据 key 路由
            //    String namespace = namespaces.split(",")[0];
            //    Config config = ConfigService.getConfig(namespace);
            //    value = config.getProperty(key, null);
            //    if (value != null) return value;
            //} catch (Exception e) {
            //    log.debug("Apollo get property failed: {}", e.getMessage());
            //}
        }

        // 3. 兜底：返回默认值 (这里假设你有一个本地的 fallback 机制，或者直接返回默认值)
        // 如果需要读取本地文件，可以在这里调用 localProps.getProperty
        return localProps.getProperty(key, defaultValue);
    }
}
