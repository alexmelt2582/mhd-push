package com.mhd.push.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检测
 *
 * @author zhao-hao-dong

 */
@Slf4j
@RestController
public class HealthController {
    @GetMapping("/")
    public String health() {
        return "success";
    }
}
