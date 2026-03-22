package org.kyc.management.bankservice.controller;

import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.kyc.management.bankservice.dto.BankKycInitiateRequest;
import org.kyc.management.bankservice.dto.BypassRequestDto;
import org.kyc.management.bankservice.dto.OtpRequestDto;
import org.kyc.management.bankservice.dto.OtpVerificationDto;
import org.kyc.management.bankservice.entity.BankCustomer;
import org.kyc.management.bankservice.service.BankService;
import org.kyc.management.bankservice.service.KycOrchestrationService;
import org.springframework.http.HttpStatus;
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


    @GetMapping("/status/{userId}")
    public ResponseEntity<BankCustomer> getStatus(@PathVariable("userId") String userId) {
        return ResponseEntity.ok(bankSvc.getCustomerStatus(userId));
    }

    @PostMapping("/bypass")
    public ResponseEntity<BankCustomer> adminBypass(@RequestBody @Valid BypassRequestDto req) {
        return ResponseEntity.ok(bankSvc.bypassKyc(req));
    }

    @PostMapping("/request-otp")
    public ResponseEntity<?> requestOTP(@RequestBody @Valid OtpRequestDto otpRequestDto) {
        if (bankSvc.sendOtp(otpRequestDto)) {
            return ResponseEntity.status(HttpStatus.CREATED).body("OTP Sent successfully ...");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("OTP Sent failed ...");
        }
    }

    @PostMapping("/otp-verification")
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
    public ResponseEntity<java.util.List<BankCustomer>> listApprovedConsents() {
        return ResponseEntity.ok(bankSvc.getApprovedConsents());
    }

    @PostMapping("/fetch/{userId}")
    public ResponseEntity<Map<String, Object>> fetchCustomerData(
            @PathVariable("userId") String userId) {
        return ResponseEntity.ok(bankSvc.fetchKycData(userId));
    }
}
