package org.rbi.consentservice.controller;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.rbi.consentservice.dto.ConsentResponseDto;
import org.rbi.consentservice.service.ConsentService;
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
