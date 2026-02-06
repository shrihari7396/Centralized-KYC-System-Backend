package org.kyc.management.kycmanagementservice.client;

import org.kyc.management.kycmanagementservice.entity.MockUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name="DigilockerService")
public interface DigilockerProfileService {
    @GetMapping("/user/{digilockerUserId}")
      MockUser getUserProfile(@PathVariable String digilockerUserId) ;
}
