package org.rbi.consentservice.client;

import org.rbi.consentservice.dto.BankDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "BANK-SERVICE")
public interface BankFeignClient {
    @GetMapping("/bank/kyc/banks/{bankId}")
    BankDto getBankById(@PathVariable("bankId") String bankId);
}
