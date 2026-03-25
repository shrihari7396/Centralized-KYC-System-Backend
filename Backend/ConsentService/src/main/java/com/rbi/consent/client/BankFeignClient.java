package com.rbi.consent.client;

import com.rbi.consent.dto.BankDto;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "BANK-SERVICE",
        url = "http://localhost:8082") // Replace with actual eureka name or configured url
public interface BankFeignClient {

    @PostMapping("/api/banks/notify-consent")
    void notifyBank(
            @RequestParam("consentId") UUID consentId, @RequestParam("jwtToken") String jwtToken);

    @GetMapping("/bank/kyc/banks/{bankId}")
    BankDto getBankById(@PathVariable("bankId") String bankId);
}
