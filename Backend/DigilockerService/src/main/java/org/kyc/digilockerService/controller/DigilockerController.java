package org.kyc.digilockerService.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kyc.digilockerService.dto.OtpGenerateRequest;
import org.kyc.digilockerService.dto.OtpVerificationDto;
import org.kyc.digilockerService.kafka.LogProducer;
import org.kyc.digilockerService.service.DigilockerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/digilocker")
@RequiredArgsConstructor
@Slf4j
public class DigilockerController {

    private final DigilockerService digilockerService;

    private final LogProducer logProducer;

    @GetMapping("/redirect")
    public ResponseEntity<?> redirectToPartner() {
        log.info("Redirecting to partner...");
        return ResponseEntity.ok(Map.of("message", "Redirected to partner. Please enter mobile number."));
    }

    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(@RequestBody OtpGenerateRequest request) {
        log.info("Requesting OTP generation for mobile: {}", request.getEmail());

        Boolean response;
        try {
            response = digilockerService.generateAndSendOtp(request);
            if(response) {
                return ResponseEntity.status(HttpStatus.CREATED).body("OTP sent Successfully ...");
            }
        } catch (Exception e) {
            log.error("Error requesting OTP from UserService", e);
            logProducer.error("OTP_REQUEST_FAILED", Map.of("email", request.getEmail(), "error", e.getMessage()));
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to send OTP."));
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Some Error occurred at our end ... ");
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpVerificationDto request) {
        log.info("Verifying OTP for email: {}", request.getEmail());
        if (!request.isConsentAccepted()) {
            logProducer.warning("OTP_VERIFICATION_REJECTED", Map.of("email", request.getEmail(), "reason", "Consent not accepted"));
            return ResponseEntity.badRequest().body(Map.of("error", "Consent must be accepted."));
        }
        
        try {
            Map<String, String> verificationRequest = Map.of(
                    "otp", request.getOtp(),
                    "email", request.getEmail()
            );
            
            Boolean response = digilockerService.verifyOtp(request.getOtp(), request.getEmail());
            
            if (response) {
                digilockerService.processVerification(request.getEmail());
                logProducer.info("OTP_VERIFICATION_SUCCESS", Map.of("email", request.getEmail()));
                return ResponseEntity.ok(Map.of("message", "Verification successful. Data is being processed."));
            } else {
                logProducer.warning("OTP_VERIFICATION_FAILED", Map.of("email", request.getEmail()));
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(Map.of("error", "OTP verification failed."));
            }
        } catch (Exception e) {
            log.error("Error verifying OTP with UserService", e);
            logProducer.error("OTP_VERIFICATION_ERROR", Map.of("mobileNumber", request.getEmail(), "error", e.getMessage()));
            return ResponseEntity.badRequest().body(Map.of("error", "OTP verification failed."));
        }
    }
}
