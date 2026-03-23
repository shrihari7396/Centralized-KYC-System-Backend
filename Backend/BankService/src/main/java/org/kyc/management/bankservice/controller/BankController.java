package org.kyc.management.bankservice.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kyc.management.bankservice.dto.BankKycInitiateRequest;
import org.kyc.management.bankservice.dto.BypassRequestDto;
import org.kyc.management.bankservice.dto.DashboardStatsResponse;
import org.kyc.management.bankservice.dto.OtpRequestDto;
import org.kyc.management.bankservice.dto.OtpVerificationDto;
import org.kyc.management.bankservice.entity.Bank;
import org.kyc.management.bankservice.entity.BankCustomer;
import org.kyc.management.bankservice.entity.BankCustomerKycData;
import org.kyc.management.bankservice.entity.Consent;
import org.kyc.management.bankservice.service.BankService;
import org.kyc.management.bankservice.service.KycOrchestrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bank/kyc")
@RequiredArgsConstructor
@Slf4j
public class BankController {

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

    @PostMapping("/otp/request")
    public ResponseEntity<?> requestOTP(@RequestBody @Valid OtpRequestDto otpRequestDto) {
        log.info("Request Arrived !!!");
        if (bankSvc.sendOtp(otpRequestDto)) {
            return ResponseEntity.status(HttpStatus.CREATED).body("OTP Sent successfully ...");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("OTP Sent failed ...");
        }
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<?> otpVerification(
            @RequestBody @Valid OtpVerificationDto otpVerificationDto) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(bankSvc.getLoginResponseDto(otpVerificationDto));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Invalid Authorization header");
        }
        String token = authHeader.substring(7);
        if (bankSvc.logout(token)) {
            return ResponseEntity.ok("Logged out successfully");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Logout failed");
        }
    }

    @GetMapping("/consents/approved")
    public ResponseEntity<List<BankCustomer>> listApprovedConsents() {
        return ResponseEntity.ok(bankSvc.getApprovedConsents());
    }

    @PostMapping("/fetch/{userId}/{bankId}")
    public ResponseEntity<Map<String, Object>> fetchCustomerData(
            @PathVariable("userId") String userId, @PathVariable("bankId") String bankId) {
        return ResponseEntity.ok(bankSvc.fetchKycData(userId, bankId));
    }

    @GetMapping("/bank/profile")
    public ResponseEntity<Bank> getBankProfile(Authentication auth) {
        return ResponseEntity.ok(bankSvc.getBankProfile(auth.getName()));
    }

    @GetMapping("/customers/{bankId}")
    public ResponseEntity<List<BankCustomer>> getCustomersByBank(
            @PathVariable("bankId") String bankId) {
        return ResponseEntity.ok(bankSvc.getCustomersByBank(bankId));
    }

    @GetMapping("/stats/{bankId}")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats(
            @PathVariable("bankId") String bankId) {
        return ResponseEntity.ok(bankSvc.getDashboardStats(bankId));
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

    @GetMapping("/banks")
    public ResponseEntity<List<Bank>> getAllBanks() {
        return ResponseEntity.ok(bankSvc.getAllBanks());
    }

    @GetMapping("/history/{userId}/{bankId}")
    public ResponseEntity<List<BankCustomerKycData>> getKycHistory(
            @PathVariable("userId") String userId, @PathVariable("bankId") String bankId) {
        return ResponseEntity.ok(bankSvc.getKycHistory(userId, bankId));
    }
}
