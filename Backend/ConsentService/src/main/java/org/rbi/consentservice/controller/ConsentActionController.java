package org.rbi.consentservice.controller;

import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.rbi.consentservice.dto.ConsentActionDto;
import org.rbi.consentservice.logging.KafkaLogProducer;
import org.rbi.consentservice.service.ConsentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/consents")
@RequiredArgsConstructor
public class ConsentActionController {

    private final ConsentService consentService;
    private final KafkaLogProducer logProducer;

    @PostMapping("/{consentId}/otp")
    public ResponseEntity<Void> sendOtp(@PathVariable String consentId) {
        consentService.sendConsentOtp(UUID.fromString(consentId));
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
}
