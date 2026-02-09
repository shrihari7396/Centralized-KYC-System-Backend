package org.kyc.digilockerService.controller;

import lombok.RequiredArgsConstructor;
import org.kyc.digilockerService.service.DigilockerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("digilocker/auth")
@RequiredArgsConstructor
public class AuthController {

    private final DigilockerService digilockerService;

    @GetMapping("/url")
    public ResponseEntity<String> getAuthUrl() {
        String state = UUID.randomUUID().toString();
        return ResponseEntity.ok(digilockerService.getAuthorizationUrl(state));
    }

    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam("code") String code, @RequestParam("state") String state) {
        return ResponseEntity.ok(digilockerService.getAccessToken(code, state));
    }
}
