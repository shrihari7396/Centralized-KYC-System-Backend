package org.kyc.management.kycmanagementservice.service;

import org.kyc.management.kycmanagementservice.DTO.UpdateStatusDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Service
@FeignClient(name="UserService")
public class updateUser {
    @GetMapping("/update-status")
    public ResponseEntity<?> updateStatus(@RequestBody UpdateStatusDto updateStatusDto);

}
