package com.rbi.userservice.service;

import com.rbi.userservice.DTO.LoginResponseDto;
import com.rbi.userservice.DTO.OtpRequestDto;
import com.rbi.userservice.DTO.OtpVerificationDto;
import com.rbi.userservice.DTO.RegisterDTO;
import com.rbi.userservice.DTO.UpdateStatusDto;

public interface UserService {
    Boolean sendOtp(OtpRequestDto otpRequestDto);
    Boolean sendOtpByPhone(String phone);

    Boolean registerUser(RegisterDTO user);

    Boolean verifyOtp(String otp, String key);

    void saveOtp(String otp, String key);

    void deleteOtp(String key);

    LoginResponseDto getLoginResponseDto(OtpVerificationDto otpVerificationDto);

    Object updateStatus(UpdateStatusDto updateStatusDto);
}
