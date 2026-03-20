package org.kyc.management.bankservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.kyc.management.bankservice.dto.BankKycInitiateRequest;
import org.kyc.management.bankservice.dto.BankKycSubmissionRequest;
import org.kyc.management.bankservice.dto.BypassRequestDto;
import org.kyc.management.bankservice.dto.KycResponseDto;
import org.kyc.management.bankservice.entity.BankCustomer;
import org.kyc.management.bankservice.service.BankService;
import org.kyc.management.bankservice.service.KycOrchestrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bank/kyc")
@RequiredArgsConstructor
public class BankController {

    private final KycOrchestrationService orchestrator;
    private final BankService bankSvc;

    @PostMapping("/initiate")
    public ResponseEntity<BankCustomer> initiate(@RequestBody @Valid BankKycInitiateRequest req) {
        return ResponseEntity.ok(orchestrator.initiateKyc(req));
    }

    @PostMapping("/submit")
    public ResponseEntity<KycResponseDto> submitData(
            @RequestBody @Valid BankKycSubmissionRequest req) {
        return ResponseEntity.ok(orchestrator.submitKycData(req));
    }

    @GetMapping("/status/{userId}")
    public ResponseEntity<BankCustomer> getStatus(@PathVariable("userId") String userId) {
        return ResponseEntity.ok(bankSvc.getCustomerStatus(userId));
    }

    @PostMapping("/bypass")
    public ResponseEntity<BankCustomer> adminBypass(@RequestBody @Valid BypassRequestDto req) {
        return ResponseEntity.ok(bankSvc.bypassKyc(req));
    }
}
