package com.mhd.push.publicapi.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查控制器。
 */
@Slf4j
@RestController
public class HealthController {

    /**
     * 提供最基础的存活探针。
     *
     * @return 固定成功标识
     */
    @GetMapping("/")
    public String health() {
        return "success";
    }
}
