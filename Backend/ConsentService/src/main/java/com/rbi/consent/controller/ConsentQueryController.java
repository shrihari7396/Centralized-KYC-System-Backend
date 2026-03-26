package com.rbi.consent.controller;

import com.rbi.consent.dto.ConsentResponseDto;
import com.rbi.consent.service.ConsentService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/consents")
@RequiredArgsConstructor
public class ConsentQueryController {

    private final ConsentService consentService;

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
