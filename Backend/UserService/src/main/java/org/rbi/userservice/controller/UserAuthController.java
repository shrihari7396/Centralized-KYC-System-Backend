package org.rbi.userservice.controller;

import org.rbi.userservice.DTO.OtpRequestDto;
import org.rbi.userservice.DTO.OtpVerificationDto;
import org.rbi.userservice.exception.UserNotFoundException;
import org.rbi.userservice.kafkaEvents.LogProducer;
import org.rbi.userservice.service.UserService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserAuthController {

    private final UserService userService;
    private final LogProducer logProducer;

    @PostMapping("/otp/request")
    public ResponseEntity<?> requestOTP(@RequestBody OtpRequestDto otpRequestDtp) {
        if (!userService.isExistByEmail(otpRequestDtp.getEmail())) {
            throw new UserNotFoundException("User Not Found !!!");
        }
        if (userService.sendOtp(otpRequestDtp)) {
            logProducer.info(
                    "OTP_REQUEST_EMAIL_SUCCESS", Map.of("email", otpRequestDtp.getEmail()));
            return ResponseEntity.status(HttpStatus.CREATED).body("OTP Sent successfully ...");
        } else {
            logProducer.warning(
                    "OTP_REQUEST_EMAIL_FAILED", Map.of("email", otpRequestDtp.getEmail()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("OTP Sent failed ...");
        }
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<?> otpVerification(@RequestBody OtpVerificationDto otpVerificationDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.getLoginResponseDto(otpVerificationDto));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Invalid Authorization header");
        }
        String token = authHeader.substring(7);
        if (userService.logout(token)) {
            return ResponseEntity.ok("Logged out successfully");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Logout failed");
        }
    }
}
