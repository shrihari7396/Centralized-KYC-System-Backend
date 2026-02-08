package org.kyc.digilockerService.controller;

import lombok.RequiredArgsConstructor;
import org.kyc.digilockerService.model.ConsentRequest;
import org.kyc.digilockerService.model.ConsentResponse;
import org.kyc.digilockerService.model.DocumentResponse;
import org.kyc.digilockerService.model.VerifiedIdentity;
import org.kyc.digilockerService.service.MockDigilockerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/digilocker")
@RequiredArgsConstructor
public class DigilockerMockController {

    private final MockDigilockerService mockDigilockerService;

    @PostMapping("/consent")
    public ResponseEntity<ConsentResponse> grantConsent(@RequestBody ConsentRequest request) {
        return ResponseEntity.ok(mockDigilockerService.grantConsent(request));
    }

    @GetMapping("/user/{digilockerUserId}")
    public ResponseEntity<VerifiedIdentity> getUserProfile(@PathVariable String digilockerUserId) {
        return ResponseEntity.ok(mockDigilockerService.getUserProfile(digilockerUserId));
    }

    @GetMapping("/documents/{digilockerUserId}")
    public ResponseEntity<DocumentResponse> getDocuments(@PathVariable String digilockerUserId) {
        return ResponseEntity.ok(mockDigilockerService.getDocuments(digilockerUserId));
    }
}
