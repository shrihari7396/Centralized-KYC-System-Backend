package org.kyc.management.kycmanagementservice.client;

import org.kyc.management.kycmanagementservice.dto.DocumentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
@FeignClient(name = "DigilockerService")
public interface DocumentFetchService {
    @GetMapping("/documents/{digilockerUserId}")
     DocumentResponse getDocuments(@PathVariable String digilockerUserId);
}
