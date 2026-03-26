package org.rbi.bankservice.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rbi.bankservice.dto.BankKycInitiateRequest;
import org.rbi.bankservice.dto.BypassRequestDto;
import org.rbi.bankservice.dto.TimelineEvent;
import org.rbi.bankservice.entity.BankCustomer;
import org.rbi.bankservice.entity.BankCustomerKycData;
import org.rbi.bankservice.entity.DocumentType;
import org.rbi.bankservice.service.BankService;
import org.rbi.bankservice.service.KycOrchestrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bank/kyc")
@RequiredArgsConstructor
@Slf4j
public class BankKycController {

    private final KycOrchestrationService orchestrator;
    private final BankService bankSvc;

    @PostMapping("/initiate")
    public ResponseEntity<BankCustomer> initiate(@RequestBody @Valid BankKycInitiateRequest req) {
        return ResponseEntity.ok(orchestrator.initiateKyc(req));
    }

    @GetMapping("/status/{userId}/{bankId}")
    public ResponseEntity<BankCustomer> getStatus(
            @PathVariable("userId") String userId, @PathVariable("bankId") String bankId) {
        return ResponseEntity.ok(bankSvc.getCustomerStatus(userId, bankId));
    }

    @PostMapping("/bypass")
    public ResponseEntity<BankCustomer> adminBypass(@RequestBody @Valid BypassRequestDto req) {
        return ResponseEntity.ok(bankSvc.bypassKyc(req));
    }

    @GetMapping("/history/{userId}/{bankId}")
    public ResponseEntity<List<BankCustomerKycData>> getKycHistory(
            @PathVariable("userId") String userId, @PathVariable("bankId") String bankId) {
        return ResponseEntity.ok(bankSvc.getKycHistory(userId, bankId));
    }

    @GetMapping("/audit/timeline/{userId}/{bankId}")
    public ResponseEntity<List<TimelineEvent>> getTimeline(
            @PathVariable String userId, @PathVariable String bankId) {
        return ResponseEntity.ok(bankSvc.getUnifiedTimeline(userId, bankId));
    }

    @GetMapping("/verification/report/{kycRequestId}")
    public ResponseEntity<Map<DocumentType, String>> getVerificationReport(
            @PathVariable Long kycRequestId) {
        return ResponseEntity.ok(bankSvc.getDetailReport(kycRequestId));
    }
}
