package com.app.med_support;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MedSupportApplication {

	public static void main(String[] args) {
		SpringApplication.run(MedSupportApplication.class, args);
	}
}
