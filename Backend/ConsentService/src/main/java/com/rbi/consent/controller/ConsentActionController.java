package com.rbi.consent.controller;

import com.rbi.consent.dto.ConsentActionDto;
import com.rbi.consent.dto.ConsentRequestDto;
import com.rbi.consent.logging.KafkaLogProducer;
import com.rbi.consent.service.ConsentService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/consents")
@RequiredArgsConstructor
public class ConsentActionController {

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

    @PostMapping("/{consentId}/submit")
    public ResponseEntity<Void> submitConsent(
            @PathVariable UUID consentId, @Valid @RequestBody ConsentActionDto actionDto) {
        consentService.submitConsent(consentId, actionDto);
        logProducer.logInfo(
                "CONSENT_SUBMITTED",
                Map.of("consentId", consentId.toString(), "action", actionDto.getAction()));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{consentId}/status")
    public ResponseEntity<Void> handleConsentStatus(
            @PathVariable UUID consentId, @RequestParam String action) {
        if ("Accept".equalsIgnoreCase(action)) {
            consentService.approveConsent(consentId);
            logProducer.logInfo("CONSENT_APPROVED", Map.of("consentId", consentId.toString()));
        } else if ("Decline".equalsIgnoreCase(action) || "Reject".equalsIgnoreCase(action)) {
            consentService.rejectConsent(consentId);
            logProducer.logInfo("CONSENT_REJECTED", Map.of("consentId", consentId.toString()));
        } else {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }
}
