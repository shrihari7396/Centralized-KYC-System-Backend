package com.rbi.userservice.controller;

import com.rbi.userservice.DTO.OtpRequestDto;
import com.rbi.userservice.DTO.OtpVerificationDto;
import com.rbi.userservice.DTO.RegisterDTO;
import com.rbi.userservice.DTO.UpdateStatusDto;
import com.rbi.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterDTO registerDto) {
        if (userService.registerUser(registerDto)) {
            return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully ...");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User registration failed ...");
        }
    }

    @PostMapping("/request-otp")
    public ResponseEntity<?> requestOTP(@RequestBody OtpRequestDto otpRequestDtp) {
        if (userService.sendOtp(otpRequestDtp)) {
            return ResponseEntity.status(HttpStatus.CREATED).body("OTP Sent successfully ...");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("OTP Sent failed ...");
        }
    }

    @PostMapping("/otp-verification")
    public ResponseEntity<?> otpVerification(@RequestBody OtpVerificationDto otpVerificationDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.getLoginResponseDto(otpVerificationDto));
    }

    @PutMapping("/updateStatus")
    public ResponseEntity<?> updateStatus(@RequestBody UpdateStatusDto updateStatusDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.updateStatus(updateStatusDto));
    }

    

}
