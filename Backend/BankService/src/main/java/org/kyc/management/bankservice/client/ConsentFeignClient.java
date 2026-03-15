package org.kyc.management.bankservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;
import java.util.Map;

@FeignClient(name = "ConsentService", path = "/consents")
public interface ConsentFeignClient {
    @GetMapping
    Map<String, Object> getConsent(@RequestParam("userId") String userId, @RequestParam("bankId") UUID bankId);

    @PostMapping("/request")
    Map<String, Object> requestConsent(@RequestBody Map<String, Object> requestDto);
}
