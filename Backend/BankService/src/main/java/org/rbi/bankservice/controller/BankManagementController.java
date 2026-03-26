package org.kyc.management.bankservice.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kyc.management.bankservice.dto.BankRegisterRequest;
import org.kyc.management.bankservice.dto.DashboardStatsResponse;
import org.kyc.management.bankservice.entity.Bank;
import org.kyc.management.bankservice.service.BankService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bank/kyc")
@RequiredArgsConstructor
@Slf4j
public class BankManagementController {

    private final BankService bankSvc;

    @GetMapping("/bank/profile")
    public ResponseEntity<Bank> getBankProfile(Authentication auth) {
        return ResponseEntity.ok(bankSvc.getBankProfile(auth.getName()));
    }

    @GetMapping("/stats/{bankId}")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats(
            @PathVariable("bankId") String bankId) {
        return ResponseEntity.ok(bankSvc.getDashboardStats(bankId));
    }

    @GetMapping("/banks")
    public ResponseEntity<List<Bank>> getAllBanks() {
        return ResponseEntity.ok(bankSvc.getAllBanks());
    }

    @GetMapping("/banks/{bankId}")
    public ResponseEntity<Bank> getBankById(@PathVariable("bankId") String bankId) {
        return ResponseEntity.ok(bankSvc.getBankById(bankId));
    }

    @PostMapping("/register")
    public ResponseEntity<Bank> registerBank(@RequestBody @Valid BankRegisterRequest req) {
        return ResponseEntity.ok(bankSvc.registerBank(req));
    }
}
