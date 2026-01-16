package com.rbi.userservice.service;

import com.rbi.userservice.DTO.RegisterDTO;

public interface UserServiceInterface {
    Boolean userLogin(String email);
    Boolean uegisterUser(RegisterDTO user);
    Boolean verifyOtp(String otp);

}
