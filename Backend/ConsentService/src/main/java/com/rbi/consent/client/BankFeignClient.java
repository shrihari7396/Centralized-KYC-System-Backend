package com.rbi.consent.client;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "BANK-SERVICE") // Replace with actual eureka name or configured url
public interface BankFeignClient {

    @PostMapping("/api/banks/notify-consent")
    void notifyBank(
            @RequestParam("consentId") UUID consentId, @RequestParam("jwtToken") String jwtToken);
}
