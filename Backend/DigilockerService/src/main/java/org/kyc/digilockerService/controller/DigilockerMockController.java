package org.kyc.digilockerService.controller;

import lombok.RequiredArgsConstructor;
import org.kyc.digilockerService.model.*;
import org.kyc.digilockerService.service.MockDigilockerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/digilocker")
@RequiredArgsConstructor
public class DigilockerMockController {

    private final MockDigilockerService mockDigilockerService;

    // User grants consent (mock)
    @PostMapping("/consent")
    public ResponseEntity<ConsentResponse> grantConsent(
            @RequestBody ConsentRequest request
    ) {
        return ResponseEntity.ok(mockDigilockerService.grantConsent(request));
    }

    // Fetch verified identity
    @GetMapping("/user/{digilockerUserId}")
    public ResponseEntity<VerifiedIdentity> getUserProfile(
            @PathVariable String digilockerUserId
    ) {
        return ResponseEntity.ok(
                mockDigilockerService.getUserProfile(digilockerUserId)
        );
    }

    // Fetch documents & extract verified address
    @GetMapping("/documents/{digilockerUserId}")
    public ResponseEntity<VerifiedAddress> getVerifiedAddress(
            @PathVariable String digilockerUserId
    ) {
        return ResponseEntity.ok(
                mockDigilockerService.extractVerifiedAddress(digilockerUserId)
        );
    }
}
