package org.kyc.management.kycmanagementservice.client;

import org.kyc.management.kycmanagementservice.dto.UpdateStatusDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name="UserService")
public interface UpdateUser {
    @PutMapping("/update-status")
     ResponseEntity<?> updateStatus(@RequestBody UpdateStatusDto updateStatusDto);
}
