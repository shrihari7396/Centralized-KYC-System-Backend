package com.rbi.consent.controller;

import com.rbi.consent.dto.ConsentRequestDto;
import com.rbi.consent.dto.ConsentResponseDto;
import com.rbi.consent.kafka.LogProducer;
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
public class ConsentController {

    private final ConsentService consentService;
    private final LogProducer logProducer;

    @PostMapping("/request")
    public ResponseEntity<Void> requestConsent(@Valid @RequestBody ConsentRequestDto requestDto) {
        consentService.requestConsent(requestDto);
        logProducer.info(
                "CONSENT_REQUEST_RECEIVED",
                Map.of(
                        "userId",
                        requestDto.getUserId().toString(),
                        "bankId",
                        requestDto.getBankId().toString()));
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/approve/{consentId}")
    public ResponseEntity<ConsentResponseDto> approveConsent(@PathVariable UUID consentId) {
        ConsentResponseDto response = consentService.approveConsent(consentId);
        logProducer.info("CONSENT_APPROVED", Map.of("consentId", consentId.toString()));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reject/{consentId}")
    public ResponseEntity<Void> rejectConsent(@PathVariable UUID consentId) {
        consentService.rejectConsent(consentId);
        logProducer.info("CONSENT_REJECTED", Map.of("consentId", consentId.toString()));
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<ConsentResponseDto> getConsent(
            @RequestParam String userId, @RequestParam UUID bankId) {
        ConsentResponseDto response = consentService.getConsent(userId, bankId);
        return ResponseEntity.ok(response);
    }
}
