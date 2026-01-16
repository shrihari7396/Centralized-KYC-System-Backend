package com.rbi.userservice.service;


import com.rbi.userservice.DTO.RegisterDTO;
import org.springframework.beans.factory.annotation.Autowired;

public class UserServiceImpl implements UserServiceInterface {

    @Autowired

    @Override
    public Boolean userLogin(String email) {
        return ;
    }

    @Override
    public Boolean uegisterUser(RegisterDTO user) {
        return ;
    }

    @Override
    public Boolean verifyOtp(String otp) {
        return null;
    }
}
