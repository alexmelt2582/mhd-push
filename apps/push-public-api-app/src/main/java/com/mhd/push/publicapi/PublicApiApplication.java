package com.mhd.push.publicapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * public-api 应用启动入口。
 */
@SpringBootApplication(scanBasePackages = "com.mhd.push")
public class PublicApiApplication {

	/**
	 * 启动 public-api 应用。
	 *
	 * @param args 启动参数
	 */
	public static void main(String[] args) {
		SpringApplication.run(PublicApiApplication.class, args);
	}
}
