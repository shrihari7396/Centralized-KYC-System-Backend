package org.rbi.kycmanagementservice.controller;

import org.rbi.kycmanagementservice.dto.DigilockerDataRequest;
import org.rbi.kycmanagementservice.entity.KycEntity;
import org.rbi.kycmanagementservice.service.KycService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kyc")
public class KycDataIngestionController {

    @Autowired private KycService kycService;

    /** Endpoint for Digilocker to POST user data. */
    @PostMapping("/digilocker")
    public ResponseEntity<KycEntity> storeDigilockerData(
            @RequestBody DigilockerDataRequest request) {
        KycEntity savedEntity = kycService.storeDigilockerData(request);
        return ResponseEntity.ok(savedEntity);
    }
}
