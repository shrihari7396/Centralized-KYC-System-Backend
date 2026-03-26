package org.rbi.bankservice.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rbi.bankservice.entity.BankCustomer;
import org.rbi.bankservice.entity.BankCustomerKycData;
import org.rbi.bankservice.entity.Consent;
import org.rbi.bankservice.service.BankService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bank/kyc")
@RequiredArgsConstructor
@Slf4j
public class BankConsentController {

    private final BankService bankSvc;

    @GetMapping("/consents/approved")
    public ResponseEntity<List<BankCustomer>> listApprovedConsents() {
        return ResponseEntity.ok(bankSvc.getApprovedConsents());
    }

    @GetMapping("/consents/pending")
    public ResponseEntity<List<Consent>> getPendingConsents(Authentication auth) {
        return ResponseEntity.ok(bankSvc.getPendingConsents(auth.getName()));
    }

    @GetMapping("/consents/all/{bankId}")
    public ResponseEntity<List<Consent>> getAllConsentsByBank(
            @PathVariable("bankId") String bankId) {
        return ResponseEntity.ok(bankSvc.getAllConsentsByBank(bankId));
    }

    @GetMapping("/consents/{consentId}/data")
    public ResponseEntity<BankCustomerKycData> getDataByConsent(@PathVariable String consentId) {
        return ResponseEntity.ok(bankSvc.findByConsentId(consentId));
    }
}
