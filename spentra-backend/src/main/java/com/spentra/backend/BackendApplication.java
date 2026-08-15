package com.spentra.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

/**
 * Main Spring Boot entry point for the Spentra backend application.
 * Enables background task scheduling for automated recurring transactions.
 */
@SpringBootApplication
@EnableScheduling
public class BackendApplication {

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
