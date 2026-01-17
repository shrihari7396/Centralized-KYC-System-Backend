package com.rbi.userservice.service;

import com.rbi.userservice.DTO.OtpRequestDto;
import com.rbi.userservice.DTO.RegisterDTO;

public interface UserServiceInterface {
    Boolean sendOtp(OtpRequestDto otpRequestDto);
    Boolean registerUser(RegisterDTO user);
    Boolean verifyOtp(String otp,String email);
    void saveOtp(String otp,String email);
    void deleteOtp(String email);

}
