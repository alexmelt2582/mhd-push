package com.mhd.push.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author zhao-hao-dong
 */
@SpringBootApplication(scanBasePackages = "com.mhd.push")
public class WorkerApplication {

	public static void main(String[] args) {
		SpringApplication.run(WorkerApplication.class, args);
	}
}
