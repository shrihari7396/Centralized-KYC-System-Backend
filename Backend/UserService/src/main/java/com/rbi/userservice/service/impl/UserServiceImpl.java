package com.rbi.userservice.service.impl;

import com.rbi.userservice.DTO.LoginResponseDto;
import com.rbi.userservice.DTO.OtpRequestDto;
import com.rbi.userservice.DTO.OtpVerificationDto;
import com.rbi.userservice.DTO.RegisterDTO;
import com.rbi.userservice.DTO.UpdateStatusDto;
import com.rbi.userservice.Model.UserEntity;
import com.rbi.userservice.Model.UserStatus;
import com.rbi.userservice.Repository.UserRepository;
import com.rbi.userservice.config.KafkaTopicNameConstants;
import com.rbi.userservice.exception.InValidOtpException;
import com.rbi.userservice.kafkaEvents.CreateOtpEvent;
import com.rbi.userservice.service.JWTService;
import com.rbi.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, CreateOtpEvent> kafkaTemplate;
    private final JWTService jwtService;
    private final UserRepository userRepository;

    @Override
    public Boolean sendOtp(OtpRequestDto otpRequestDto) {
        return generateAndSendOtp(otpRequestDto.getFirstName(), otpRequestDto.getLastName(), otpRequestDto.getEmail(), otpRequestDto.getEmail());
    }

    @Override
    public Boolean sendOtpByPhone(String phone) {
        UserEntity user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("User not found with phone: " + phone));
        return generateAndSendOtp(user.getFirstName(), user.getLastName(), user.getEmail(), phone);
    }

    private Boolean generateAndSendOtp(String firstName, String lastName, String email, String key) {
        SecureRandom secureRandom = new SecureRandom();
        int otp = secureRandom.nextInt(1_000_000);
        String finalotp = String.format("%06d", otp);
        
        CreateOtpEvent event = CreateOtpEvent.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .otp(finalotp)
                .build();
        
        saveOtp(finalotp, key);
        kafkaTemplate.send(KafkaTopicNameConstants.OTP_CREATED_TOPIC, event);
        return true;
    }

    @Override
    public Boolean registerUser(RegisterDTO user) {
        UserEntity u = UserEntity.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .status(UserStatus.KYC_PENDING)
                .phone(user.getMobileNumber()) // Fixed field name
                .build();

        return userRepository.save(u) != null;
    }

    @Override
    public Boolean verifyOtp(String otp, String key) {
        String StoredOtp = redisTemplate.opsForValue().get(key);
        if (StoredOtp == null) {
            throw new InValidOtpException("Expired or invalid OTP");
        }
        boolean result = StoredOtp.equals(otp);
        if (!result) {
            throw new InValidOtpException("Invalid OTP");
        }
        deleteOtp(key);
        return result;
    }

    @Override
    public void saveOtp(String otp, String key) {
        redisTemplate.opsForValue().set(key, otp, Duration.ofMinutes(10));
    }

    @Override
    public void deleteOtp(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public LoginResponseDto getLoginResponseDto(OtpVerificationDto otpVerificationDto) {
        LoginResponseDto response = null;

        if (verifyOtp(otpVerificationDto.getOtp(), otpVerificationDto.getEmail())) {
            String token = jwtService.generateToken(otpVerificationDto.getEmail(), otpVerificationDto.getRole().toString());
            response = LoginResponseDto.builder()
                    .role(otpVerificationDto.getRole())
                    .email(otpVerificationDto.getEmail())
                    .firstName(otpVerificationDto.getFirstName())
                    .lastName(otpVerificationDto.getLastName())
                    .token(token)
                    .build();
        }

        if (response == null) {
            throw new InValidOtpException("Invalid OTP");
        }
        return response;
    }

    @Override
    public Object updateStatus(UpdateStatusDto updateStatusDto) {
        UserEntity user = userRepository.findById(updateStatusDto.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + updateStatusDto.getEmail()));
        user.setStatus(updateStatusDto.getStatus());
        return userRepository.save(user);
    }

}
