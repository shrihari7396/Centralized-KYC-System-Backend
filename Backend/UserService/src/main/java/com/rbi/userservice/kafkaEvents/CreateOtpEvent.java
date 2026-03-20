package com.rbi.userservice.kafkaEvents;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CreateOtpEvent {
  private String firstName;
  private String lastName;
  private String email;
  private String otp;
  private String serviceType;
}
