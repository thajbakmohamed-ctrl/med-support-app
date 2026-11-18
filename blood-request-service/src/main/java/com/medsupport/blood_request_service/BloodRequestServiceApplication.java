package com.medsupport.blood_request_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.cache.annotation.EnableCaching
public class BloodRequestServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BloodRequestServiceApplication.class, args);
	}

}
