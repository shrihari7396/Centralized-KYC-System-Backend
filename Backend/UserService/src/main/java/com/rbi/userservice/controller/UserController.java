package com.rbi.userservice.controller;

import com.rbi.userservice.DTO.OtpRequestDto;
import com.rbi.userservice.DTO.OtpVerificationDto;
import com.rbi.userservice.DTO.RegisterDTO;
import com.rbi.userservice.Model.UpdateStatusDto;
import com.rbi.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
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

    @PostMapping("/requestOtp")
    public ResponseEntity<?> requestOTP(@RequestBody OtpRequestDto otpRequestDtp) {
        if (userService.sendOtp(otpRequestDtp)) {
            return ResponseEntity.status(HttpStatus.CREATED).body("OTP Sent successfully ...");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("OTP Sent failed ...");
        }
    }

    @PostMapping("/otpVerification")
    public ResponseEntity<?> otpVerification(@RequestBody OtpVerificationDto otpVerificationDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.getLoginResponseDto(otpVerificationDto));
    }

    @PostMapping("/updateStatus")
    public ResponseEntity<?> updateStatus(@RequestBody UpdateStatusDto updateStatusDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.updateStatus(updateStatusDto));
    }

}
