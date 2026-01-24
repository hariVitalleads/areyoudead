package com.areyoudead;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AreYouDeadApplication {
	public static void main(String[] args) {
		SpringApplication.run(AreYouDeadApplication.class, args);
	}
}

