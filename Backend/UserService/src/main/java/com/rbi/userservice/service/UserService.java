package com.rbi.userservice.service;

import com.rbi.userservice.DTO.LoginResponseDto;
import com.rbi.userservice.DTO.OtpRequestDto;
import com.rbi.userservice.DTO.OtpVerificationDto;
import com.rbi.userservice.DTO.RegisterDTO;
import com.rbi.userservice.Model.UpdateStatusDto;

public interface UserService {
    Boolean sendOtp(OtpRequestDto otpRequestDto);

    Boolean registerUser(RegisterDTO user);

    Boolean verifyOtp(String otp, String email);

    void saveOtp(String otp, String email);

    void deleteOtp(String email);

    LoginResponseDto getLoginResponseDto(OtpVerificationDto otpVerificationDto);

    Object updateStatus(UpdateStatusDto updateStatusDto);
}
