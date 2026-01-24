package org.kyc.management.kycmanagementservice.service;

import org.kyc.management.kycmanagementservice.DTO.UpdateStatusDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name="UserService")
public interface updateUser {
    @PutMapping("/update-status")
     ResponseEntity<?> updateStatus(@RequestBody UpdateStatusDto updateStatusDto);

}
