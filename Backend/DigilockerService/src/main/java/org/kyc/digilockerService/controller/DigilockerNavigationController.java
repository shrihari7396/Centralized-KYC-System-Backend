package org.kyc.digilockerService.controller;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/digilocker")
@Slf4j
public class DigilockerNavigationController {

    @GetMapping("/redirect")
    public ResponseEntity<?> redirectToPartner() {
        log.info("Redirecting to partner...");
        return ResponseEntity.ok(
                Map.of("message", "Redirected to partner. Please enter mobile number."));
    }
}
