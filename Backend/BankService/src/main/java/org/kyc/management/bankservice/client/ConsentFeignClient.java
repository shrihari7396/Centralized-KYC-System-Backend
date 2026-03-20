package org.kyc.management.bankservice.client;

import java.util.Map;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ConsentService", path = "/consents")
public interface ConsentFeignClient {
    @GetMapping
    Map<String, Object> getConsent(
            @RequestParam("userId") String userId, @RequestParam("bankId") UUID bankId);

    @PostMapping("/request")
    Map<String, Object> requestConsent(@RequestBody Map<String, Object> requestDto);
}
