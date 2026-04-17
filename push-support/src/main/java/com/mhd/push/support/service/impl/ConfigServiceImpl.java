package com.mhd.push.support.service.impl;

import cn.hutool.setting.dialect.Props;
import com.mhd.push.support.service.ConfigService;
import com.mhd.push.support.utils.NacosUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * @author zhao-hao-dong
 */
@Service
public class ConfigServiceImpl implements ConfigService {
    private static final String PROPERTIES_PATH = "local.properties";
    private final Props PROPS = new Props(PROPERTIES_PATH, StandardCharsets.UTF_8);

    @Value("${mhd.nacos.enable}")
    private Boolean enableNacos;
    @Resource
    private NacosUtils nacosUtils;

    @Override
    public String getProperty(String key, String defaultValue) {
        if (Boolean.TRUE.equals(enableNacos)) {
            return nacosUtils.getProperty(key, defaultValue);
        } else {
            return PROPS.getProperty(key, defaultValue);
        }
    }

    ///**
    // * apollo配置
    // */
    //@Value("${apollo.bootstrap.enabled}")
    //private Boolean enableApollo;
    //@Value("${apollo.bootstrap.namespaces}")
    //private String namespaces;
    ///**
    // * nacos配置
    // */
    //@Value("${austin.nacos.enabled}")
    //private Boolean enableNacos;
    //@Autowired
    //private NacosUtils nacosUtils;
    //
    //
    //@Override
    //public String getProperty(String key, String defaultValue) {
    //    if (Boolean.TRUE.equals(enableApollo)) {
    //        Config config = com.ctrip.framework.apollo.ConfigService.getConfig(namespaces.split(StrPool.COMMA)[0]);
    //        return config.getProperty(key, defaultValue);
    //    } else if (Boolean.TRUE.equals(enableNacos)) {
    //        return nacosUtils.getProperty(key, defaultValue);
    //    } else {
    //        return PROPS.getProperty(key, defaultValue);
    //    }
    //}
}
