package org.kyc.management.bankservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kyc.management.bankservice.dto.OtpRequestDto;
import org.kyc.management.bankservice.dto.OtpVerificationDto;
import org.kyc.management.bankservice.service.BankService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bank/kyc")
@RequiredArgsConstructor
@Slf4j
public class BankAuthController {

    private final BankService bankSvc;

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
}
