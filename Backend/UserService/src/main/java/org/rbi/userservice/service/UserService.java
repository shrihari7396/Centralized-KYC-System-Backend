package org.rbi.userservice.service;

import org.rbi.userservice.DTO.*;
import org.rbi.userservice.Model.UserStatus;

public interface UserService {
    Boolean sendOtp(OtpRequestDto otpRequestDto);

    Boolean registerUser(RegisterDTO user);

    Boolean verifyOtp(String otp, String key);

    boolean isExistByEmail(String email);

    LoginResponseDto getLoginResponseDto(OtpVerificationDto otpVerificationDto);

    Boolean logout(String token);

    UserStatus getUserStatusByEmail(String email);

    UserDto getUserDetails(String email);

    UserDto getByUserId(String userId);
}
