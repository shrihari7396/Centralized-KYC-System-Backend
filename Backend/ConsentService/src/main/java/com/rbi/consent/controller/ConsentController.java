package com.rbi.consent.controller;

import com.rbi.consent.dto.ConsentRequestDto;
import com.rbi.consent.dto.ConsentResponseDto;
import com.rbi.consent.logging.KafkaLogProducer;
import com.rbi.consent.service.ConsentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/consents")
@RequiredArgsConstructor
public class ConsentController {

    private final ConsentService consentService;
    private final KafkaLogProducer logProducer;

    @PostMapping("/request")
    public ResponseEntity<Void> requestConsent(@Valid @RequestBody ConsentRequestDto requestDto) {
        consentService.requestConsent(requestDto);
        logProducer.logInfo(
                "CONSENT_REQUEST_RECEIVED",
                Map.of(
                        "userId",
                        requestDto.getUserId().toString(),
                        "bankId",
                        requestDto.getBankId().toString()));
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/{consentId}/otp")
    public ResponseEntity<Void> sendOtp(@PathVariable UUID consentId) {
        consentService.sendConsentOtp(consentId);
        logProducer.logInfo("CONSENT_OTP_SENT", Map.of("consentId", consentId.toString()));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{consentId}/approve")
    public ResponseEntity<ConsentResponseDto> approveConsent(
            @PathVariable UUID consentId, @RequestParam String otp) {
        ConsentResponseDto response = consentService.approveConsent(consentId, otp);
        logProducer.logInfo("CONSENT_APPROVED", Map.of("consentId", consentId.toString()));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{consentId}/reject")
    public ResponseEntity<Void> rejectConsent(
            @PathVariable UUID consentId, @RequestParam String otp) {
        consentService.rejectConsent(consentId, otp);
        logProducer.logInfo("CONSENT_REJECTED", Map.of("consentId", consentId.toString()));
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/{consentId}/status")
    public ResponseEntity<Void> handleConsentStatus(
            @PathVariable UUID consentId, @RequestParam String action, @RequestParam String otp) {
        if ("Accept".equalsIgnoreCase(action)) {
            consentService.approveConsent(consentId, otp);
            logProducer.logInfo("CONSENT_APPROVED", Map.of("consentId", consentId.toString()));
        } else if ("Decline".equalsIgnoreCase(action) || "Reject".equalsIgnoreCase(action)) {
            consentService.rejectConsent(consentId, otp);
            logProducer.logInfo("CONSENT_REJECTED", Map.of("consentId", consentId.toString()));
        } else {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ConsentResponseDto>> getAllConsents(@PathVariable String userId) {
        List<ConsentResponseDto> response = consentService.getAllConsents(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ConsentResponseDto> getConsent(
            @RequestParam String userId, @RequestParam UUID bankId) {
        ConsentResponseDto response = consentService.getConsent(userId, bankId);
        return ResponseEntity.ok(response);
    }
}
