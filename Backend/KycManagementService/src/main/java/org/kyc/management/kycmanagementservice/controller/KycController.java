package org.kyc.management.kycmanagementservice.controller;

import java.util.List;
import java.util.Map;
import org.kyc.management.kycmanagementservice.dto.DigilockerDataRequest;
import org.kyc.management.kycmanagementservice.dto.DocumentProofResponseDto;
import org.kyc.management.kycmanagementservice.dto.KycTokenResponseDto;
import org.kyc.management.kycmanagementservice.dto.UserProfileResponseDto;
import org.kyc.management.kycmanagementservice.entity.KycEntity;
import org.kyc.management.kycmanagementservice.service.KycService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kyc")
public class KycController {

    @Autowired private KycService kycService;

    /** Endpoint for Digilocker to POST user data. */
    @PostMapping("/digilocker")
    public ResponseEntity<KycEntity> storeDigilockerData(
            @RequestBody DigilockerDataRequest request) {
        KycEntity savedEntity = kycService.storeDigilockerData(request);
        return ResponseEntity.ok(savedEntity);
    }

    /** Endpoint for Banks to fetch user documents dynamically relying on JWT fields. */
    @GetMapping("/bank/documents")
    public ResponseEntity<?> getDocumentsForBank(@RequestHeader("Authorization") String token) {
        try {
            Map<String, Object> data = kycService.getVerifiedDataForBank(token);
            return ResponseEntity.ok(data);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("An error occurred processing the request");
        }
    }

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
    public ResponseEntity<List<DocumentProofResponseDto>> getDocumentProofs(@PathVariable String userId) {
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
