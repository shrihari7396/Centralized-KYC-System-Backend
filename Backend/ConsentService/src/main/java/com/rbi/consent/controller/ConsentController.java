package com.rbi.consent.controller;

import com.rbi.consent.dto.ConsentRequestDto;
import com.rbi.consent.dto.ConsentResponseDto;
import com.rbi.consent.service.ConsentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/consents")
@RequiredArgsConstructor
public class ConsentController {

    private final ConsentService consentService;

    @PostMapping("/request")
    public ResponseEntity<Void> requestConsent(@Valid @RequestBody ConsentRequestDto requestDto) {
        consentService.requestConsent(requestDto);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/approve/{consentId}")
    public ResponseEntity<ConsentResponseDto> approveConsent(@PathVariable UUID consentId) {
        ConsentResponseDto response = consentService.approveConsent(consentId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reject/{consentId}")
    public ResponseEntity<Void> rejectConsent(@PathVariable UUID consentId) {
        consentService.rejectConsent(consentId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<ConsentResponseDto> getConsent(@RequestParam String userId, @RequestParam UUID bankId) {
        ConsentResponseDto response = consentService.getConsent(userId, bankId);
        return ResponseEntity.ok(response);
    }
}
