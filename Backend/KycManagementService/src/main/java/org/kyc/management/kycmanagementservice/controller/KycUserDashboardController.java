package org.kyc.management.kycmanagementservice.controller;

import java.util.List;
import org.kyc.management.kycmanagementservice.dto.DocumentProofResponseDto;
import org.kyc.management.kycmanagementservice.dto.KycTokenResponseDto;
import org.kyc.management.kycmanagementservice.dto.UserProfileResponseDto;
import org.kyc.management.kycmanagementservice.service.KycService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kyc")
public class KycUserDashboardController {

    @Autowired private KycService kycService;

    /** ----------------- User Frontend Transparency Dashboard APIs ----------------- */
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponseDto> getUserProfile(@PathVariable String userId) {
        try {
            UserProfileResponseDto profile = kycService.getUserProfile(userId);
            return ResponseEntity.ok(profile);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{userId}/documents")
    public ResponseEntity<List<DocumentProofResponseDto>> getDocumentProofs(
            @PathVariable String userId) {
        try {
            List<DocumentProofResponseDto> proofs = kycService.getDocumentProofs(userId);
            return ResponseEntity.ok(proofs);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{userId}/token")
    public ResponseEntity<KycTokenResponseDto> getKycToken(@PathVariable String userId) {
        try {
            KycTokenResponseDto token = kycService.getKycToken(userId);
            return ResponseEntity.ok(token);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{userId}/token/revoke")
    public ResponseEntity<Void> revokeKycToken(@PathVariable String userId) {
        try {
            kycService.revokeKycToken(userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
