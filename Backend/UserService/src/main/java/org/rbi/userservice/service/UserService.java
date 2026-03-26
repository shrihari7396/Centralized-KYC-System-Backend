package org.rbi.userservice.service;

import com.rbi.userservice.DTO.*;
import org.rbi.userservice.DTO.*;
import org.rbi.userservice.Model.UserStatus;

public interface UserService {
    Boolean sendOtp(OtpRequestDto otpRequestDto);

    Boolean sendOtpByPhone(String phone);

    Boolean registerUser(RegisterDTO user);

    Boolean verifyOtp(String otp, String key);

    void saveOtp(String otp, String key);

    void deleteOtp(String key);

    boolean isExistByEmail(String email);

    LoginResponseDto getLoginResponseDto(OtpVerificationDto otpVerificationDto);

    Boolean logout(String token);

    UserStatus getUserStatusByEmail(String email);

    UserDto getUserDetails(String email);
}
