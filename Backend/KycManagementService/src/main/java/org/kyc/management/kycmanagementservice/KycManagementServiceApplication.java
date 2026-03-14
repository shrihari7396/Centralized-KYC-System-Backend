package org.kyc.management.kycmanagementservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class KycManagementServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(KycManagementServiceApplication.class, args);
	}

}
