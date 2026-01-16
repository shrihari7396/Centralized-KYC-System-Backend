package com.rbi.userservice.service;


import com.rbi.userservice.DTO.RegisterDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

public class UserServiceImpl implements UserServiceInterface {

    private final RedisTemplate<String,String> redisTemplate;

    public UserServiceImpl(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Boolean userLogin(String email) {
        return null;
    }

    @Override
    public Boolean uegisterUser(RegisterDTO user) {
        return null;
    }

    @Override
    public Boolean verifyOtp(String otp,String email) {
        return null;
    }

    @Override
    public void saveOtp(String otp,String email) {
        redisTemplate.opsForValue().set(email,otp, Duration.ofMinutes(10));
    }

    @Override
    public void deleteOtp(String otp) {

    }


}
