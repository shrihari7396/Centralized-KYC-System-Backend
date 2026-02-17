package org.kyc.digilockerService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class DigilockerServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DigilockerServiceApplication.class, args);
	}

}
