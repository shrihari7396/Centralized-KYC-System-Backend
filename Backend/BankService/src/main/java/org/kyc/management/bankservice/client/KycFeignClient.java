package org.kyc.management.bankservice.client;

import org.kyc.management.bankservice.dto.KycResponseDto;
import org.kyc.management.bankservice.dto.KycSubmissionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "KYCMANAGEMENTSERVICE", path = "/api/kyc")
public interface KycFeignClient {
    @PostMapping("/submit")
    KycResponseDto submitKyc(
            @RequestBody KycSubmissionDto submissionDto,
            @RequestHeader("Authorization") String token);

    @org.springframework.web.bind.annotation.GetMapping("/bank/documents")
    java.util.Map<String, Object> fetchKycData(@RequestHeader("Authorization") String consentToken);
}
