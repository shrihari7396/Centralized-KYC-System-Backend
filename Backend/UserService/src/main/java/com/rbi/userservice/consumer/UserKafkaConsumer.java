package com.rbi.userservice.consumer;

import com.rbi.userservice.Model.UserEntity;
import com.rbi.userservice.Model.UserStatus;
import com.rbi.userservice.Repository.UserRepository;
import com.rbi.userservice.event.KycVerificationSuccessEvent;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserKafkaConsumer {

  private final UserRepository userRepository;

  private static final String SUCCESS_TOPIC = "kyc-verification-success";

  @KafkaListener(topics = SUCCESS_TOPIC, groupId = "user-service-group")
  public void consumeKycSuccess(KycVerificationSuccessEvent event) {
    log.info("Consumed KYC success event for mobile: {}", event.getMobileNumber());

    Optional<UserEntity> userOpt = userRepository.findByPhone(event.getMobileNumber());
    if (userOpt.isPresent()) {
      UserEntity user = userOpt.get();
      user.setStatus(UserStatus.ACTIVE);
      userRepository.save(user);
      log.info("User status updated to ACTIVE for phone: {}", event.getMobileNumber());
    } else {
      log.warn("User not found for phone: {}", event.getMobileNumber());
    }
  }
}
