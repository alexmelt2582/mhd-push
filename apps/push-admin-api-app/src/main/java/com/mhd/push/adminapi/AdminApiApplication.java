package com.mhd.push.adminapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author zhao-hao-dong
 */
@SpringBootApplication(scanBasePackages = "com.mhd.push")
public class AdminApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(AdminApiApplication.class, args);
	}
}
