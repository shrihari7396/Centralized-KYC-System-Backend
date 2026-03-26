package org.rbi.kycmanagementservice.controller;

import java.util.Map;
import org.rbi.kycmanagementservice.service.KycService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kyc")
public class KycBankAccessController {

    @Autowired private KycService kycService;

    /** Endpoint for Banks to fetch user documents dynamically relying on JWT fields. */
    @GetMapping("/bank/documents")
    public ResponseEntity<?> getDocumentsForBank(@RequestHeader("Authorization") String token) {
        try {
            Map<String, Object> data = kycService.getVerifiedDataForBank(token);
            return ResponseEntity.ok(data);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("An error occurred processing the request");
        }
    }
}
