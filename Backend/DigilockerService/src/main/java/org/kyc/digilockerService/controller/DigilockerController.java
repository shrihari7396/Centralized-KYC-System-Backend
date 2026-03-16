package org.kyc.digilockerService.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.kyc.digilockerService.client.UserClient;
import org.kyc.digilockerService.dto.OtpGenerateRequest;
import org.kyc.digilockerService.dto.OtpVerifyRequest;
import org.kyc.digilockerService.kafka.LogProducer;
import org.kyc.digilockerService.service.DigilockerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/digilocker")
@RequiredArgsConstructor
@Slf4j
public class DigilockerController {

    private final DigilockerService digilockerService;
    private final UserClient userClient;
    private final LogProducer logProducer;

    @GetMapping("/redirect")
    public ResponseEntity<?> redirectToPartner() {
        log.info("Redirecting to partner...");
        return ResponseEntity.ok(Map.of("message", "Redirected to partner. Please enter mobile number."));
    }

    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(@RequestBody OtpGenerateRequest request) {
        log.info("Requesting OTP generation for mobile: {}", request.getMobileNumber());
        logProducer.info("OTP_REQUEST_INITIATED", Map.of("mobileNumber", request.getMobileNumber()));
        try {
            ResponseEntity<String> response = userClient.requestOTPByPhone(request.getMobileNumber());
            logProducer.info("OTP_REQUEST_SUCCESS", Map.of("mobileNumber", request.getMobileNumber()));
            return ResponseEntity.status(response.getStatusCode()).body(Map.of("message", response.getBody()));
        } catch (Exception e) {
            log.error("Error requesting OTP from UserService", e);
            logProducer.error("OTP_REQUEST_FAILED", Map.of("mobileNumber", request.getMobileNumber(), "error", e.getMessage()));
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to send OTP."));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpVerifyRequest request) {
        log.info("Verifying OTP for mobile: {}", request.getMobileNumber());
        if (!request.isConsentAccepted()) {
            logProducer.warning("OTP_VERIFICATION_REJECTED", Map.of("mobileNumber", request.getMobileNumber(), "reason", "Consent not accepted"));
            return ResponseEntity.badRequest().body(Map.of("error", "Consent must be accepted."));
        }
        
        try {
            Map<String, String> verificationRequest = Map.of(
                    "otp", request.getOtp(),
                    "email", request.getMobileNumber()
            );
            
            ResponseEntity<Object> response = userClient.otpVerification(verificationRequest);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                digilockerService.processVerification(request.getMobileNumber());
                logProducer.info("OTP_VERIFICATION_SUCCESS", Map.of("mobileNumber", request.getMobileNumber()));
                return ResponseEntity.ok(Map.of("message", "Verification successful. Data is being processed."));
            } else {
                logProducer.warning("OTP_VERIFICATION_FAILED", Map.of("mobileNumber", request.getMobileNumber()));
                return ResponseEntity.status(response.getStatusCode()).body(Map.of("error", "OTP verification failed."));
            }
        } catch (Exception e) {
            log.error("Error verifying OTP with UserService", e);
            logProducer.error("OTP_VERIFICATION_ERROR", Map.of("mobileNumber", request.getMobileNumber(), "error", e.getMessage()));
            return ResponseEntity.badRequest().body(Map.of("error", "OTP verification failed."));
        }
    }
}
