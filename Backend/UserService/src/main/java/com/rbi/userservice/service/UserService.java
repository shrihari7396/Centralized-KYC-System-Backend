package com.rbi.userservice.service;

import com.rbi.userservice.DTO.*;
import com.rbi.userservice.Model.UserStatus;

public interface UserService {
    Boolean sendOtp(OtpRequestDto otpRequestDto);

    Boolean sendOtpByPhone(String phone);

    Boolean registerUser(RegisterDTO user);

    Boolean verifyOtp(String otp, String key);

    void saveOtp(String otp, String key);

    void deleteOtp(String key);

    boolean isExistByEmail(String email);

    LoginResponseDto getLoginResponseDto(OtpVerificationDto otpVerificationDto);

    UserDto getUserDetails(String email);

    Boolean logout(String token);

    UserStatus getUserStatusByEmail(String email);
}
