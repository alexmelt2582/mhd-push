package com.mhd.push;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author zhao-hao-dong

 */
@SpringBootApplication
@Slf4j
public class ServerApplication implements CommandLineRunner {
    @Value("${server.port}")
    private Integer serverPort;

    public static void main(String[] args) {
        new SpringApplication(ServerApplication.class).run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("push start succeeded, Index >> http://127.0.0.1:{}/", serverPort);
    }
}
