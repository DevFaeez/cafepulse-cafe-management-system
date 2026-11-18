package com.cafemanagement.cafeprediction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Profiles;

@SpringBootApplication
public class CafePredictionApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(CafePredictionApplication.class, args);
		if (context.getEnvironment().acceptsProfiles(Profiles.of("bootstrap-manager"))) {
			context.close();
		}
	}

}
