package com.rbi.userservice.controller;

import com.rbi.userservice.DTO.OtpRequestDto;
import com.rbi.userservice.DTO.OtpVerificationDto;
import com.rbi.userservice.DTO.RegisterDTO;
import com.rbi.userservice.exception.UserNotFoundException;
import com.rbi.userservice.kafkaEvents.LogProducer;
import com.rbi.userservice.service.UserService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final LogProducer logProducer;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterDTO registerDto) {
        if (userService.registerUser(registerDto)) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("User registered successfully ...");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("User registration failed ...");
        }
    }

    @PostMapping("/otp/request")
    public ResponseEntity<?> requestOTP(@RequestBody OtpRequestDto otpRequestDtp) {
        if (!userService.isExistByEmail(otpRequestDtp.getEmail())) {
            throw new UserNotFoundException("User Not Found !!!");
        }
        if (userService.sendOtp(otpRequestDtp)) {
            logProducer.info(
                    "OTP_REQUEST_EMAIL_SUCCESS", Map.of("email", otpRequestDtp.getEmail()));
            ;
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

    @GetMapping("/details")
    public ResponseEntity<?> getUserDetails(@RequestParam String email) {
        return ResponseEntity.ok(userService.getUserDetails(email));
    }

    @GetMapping("/kyc-status/{email}")
    public ResponseEntity<?> getStatusByEmail(@PathVariable String email) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getUserStatusByEmail(email));
    }
}
