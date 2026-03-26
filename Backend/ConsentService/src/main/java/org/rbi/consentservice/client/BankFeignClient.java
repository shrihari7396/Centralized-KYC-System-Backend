package org.rbi.consentservice.client;

import java.util.UUID;
import org.rbi.consentservice.dto.BankDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "BANK-SERVICE")
public interface BankFeignClient {

    @PostMapping("/api/banks/notify-consent")
    void notifyBank(
            @RequestParam("consentId") UUID consentId, @RequestParam("jwtToken") String jwtToken);

    @GetMapping("/bank/kyc/banks/{bankId}")
    BankDto getBankById(@PathVariable("bankId") String bankId);
}
