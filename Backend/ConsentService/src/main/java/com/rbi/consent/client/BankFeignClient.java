package com.rbi.consent.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "BANK-SERVICE", url = "http://localhost:8082") // Replace with actual eureka name or configured url
public interface BankFeignClient {

    @PostMapping("/api/banks/notify-consent")
    void notifyBank(@RequestParam("consentId") UUID consentId, @RequestParam("jwtToken") String jwtToken);
}
