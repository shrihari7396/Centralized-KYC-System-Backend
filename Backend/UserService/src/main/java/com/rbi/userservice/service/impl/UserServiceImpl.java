package com.rbi.userservice.service.impl;


import com.rbi.userservice.DTO.OtpRequestDto;
import com.rbi.userservice.DTO.RegisterDTO;
import com.rbi.userservice.Model.UserEntity;
import com.rbi.userservice.Model.UserStatus;
import com.rbi.userservice.Repository.UserRepository;
import com.rbi.userservice.config.KafkaTopicNameConstants;
import com.rbi.userservice.exception.InValidOtpException;
import com.rbi.userservice.kafkaEvents.CreateOtpEvent;
import com.rbi.userservice.service.UserServiceInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserServiceInterface {

    private final SecureRandom secureRandom;

    private final RedisTemplate<String,String> redisTemplate;

    private final KafkaTemplate<String, CreateOtpEvent> kafkaTemplate;

    private final UserRepository userRepository
            ;
    @Override
    public Boolean sendOtp(OtpRequestDto otpRequestDto) {
        int otp = secureRandom.nextInt(1_000_000);
        String finalotp = String.format("%06d", otp);

//          logic for kafka to send
        CreateOtpEvent event = CreateOtpEvent.builder()
                .firstName(otpRequestDto.getFirstName())
                .lastName(otpRequestDto.getLastName())
                .email(otpRequestDto.getEmail())
                .otp(finalotp)
                .build();
            kafkaTemplate.send(KafkaTopicNameConstants.OTP_CREATED_EVENT, event);

//            Redis Caching
            saveOtp(finalotp, otpRequestDto.getEmail());
            return true;
    }

    @Override
    public Boolean registerUser(RegisterDTO user) {
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
