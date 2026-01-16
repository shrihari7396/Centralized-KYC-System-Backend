package com.rbi.userservice.service;


import com.rbi.userservice.DTO.RegisterDTO;
import com.rbi.userservice.Model.UserEntity;
import com.rbi.userservice.Model.UserStatus;
import com.rbi.userservice.Repository.UserRepository;
import com.rbi.userservice.config.KafkaTopicNameConstants;
import com.rbi.userservice.exception.InValidOtpException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserServiceInterface {

    private final RedisTemplate<String,String> redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;


    @Autowired
    UserRepository userRepository;
    @Override
    public Boolean sendOtp(String email) {
          SecureRandom secureRandom = new SecureRandom();
            int otp = secureRandom.nextInt(1_000_000);
            String finalotp = String.format("%06d", otp);
//          logic for kafka to send
            kafkaTemplate.send(KafkaTopicNameConstants.OTP_CREATED_EVENT, finalotp);
            saveOtp(finalotp, email);
            return true;
    }

    @Override
    public Boolean usergisterUser(RegisterDTO user) {
        UserEntity u = UserEntity.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .status(UserStatus.KYC_PENDING)
                .mobileNumber(user.getMobileNumber())
                .build();

        return userRepository.save(u) != null;
    }

    @Override
    public Boolean verifyOtp(String otp,String email) {
      String StoredOtp = redisTemplate.opsForValue().get(email);
      boolean result = StoredOtp.equals(otp);
      if(StoredOtp == null || !result){
          throw new InValidOtpException("Invalid OTP");
      }
      deleteOtp(email);
      return result;
    }

    @Override
    public void saveOtp(String otp,String email) {
        redisTemplate.opsForValue().set(email,otp, Duration.ofMinutes(10));
    }

    @Override
    public void deleteOtp(String email) {
        redisTemplate.delete(email);
    }


}
