package org.kyc.management.kycmanagementservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name="DigilockerService")
public interface FetchKycService {
    @GetMapping("/")
}
