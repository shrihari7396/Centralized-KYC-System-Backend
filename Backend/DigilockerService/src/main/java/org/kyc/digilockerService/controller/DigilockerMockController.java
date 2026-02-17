package org.kyc.digilockerService.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.kyc.digilockerService.dto.*;
import org.kyc.digilockerService.model.DocumentType;
import org.kyc.digilockerService.service.DigilockerMockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/digilocker")
@RequiredArgsConstructor
@Tag(name = "DigiLocker Mock", description = "Simulates Government KYC Verification APIs")
public class DigilockerMockController {

    private final DigilockerMockService mockService;

    @PostMapping("/otp/generate")
    @Operation(summary = "Generate OTP", description = "Initiate verification by generating an OTP for the given document.")
    public ResponseEntity<OtpGenerateResponse> generateOtp(@Valid @RequestBody OtpGenerateRequest request) {
        return ResponseEntity.ok(mockService.generateOtp(request));
    }

    @PostMapping("/otp/verify")
    @Operation(summary = "Verify OTP", description = "Verify the OTP and receive a verification token.")
    public ResponseEntity<OtpVerifyResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return ResponseEntity.ok(mockService.verifyOtp(request));
    }

    @GetMapping("/document")
    @Operation(summary = "Fetch Verified Document", description = "Get the dummy verified document data using the verification token.")
    public ResponseEntity<DocumentResponse> getDocument(
            @RequestHeader("Authorization") String resHeader,
            @RequestParam DocumentType documentType,
            @RequestParam String documentNumber) {
        String token = resHeader.replace("Bearer ", "");
        return ResponseEntity.ok(mockService.fetchDocument(token, documentType, documentNumber));
    }

    @GetMapping("/health")
    @Operation(summary = "Health Check", description = "Check if the service is up.")
    public ResponseEntity<HealthStatus> health() {
        return ResponseEntity.ok(new HealthStatus("UP"));
    }
}
