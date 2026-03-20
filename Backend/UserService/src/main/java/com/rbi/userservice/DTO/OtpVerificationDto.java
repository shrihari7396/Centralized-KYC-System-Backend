package com.rbi.userservice.DTO;

import com.rbi.userservice.Model.Role;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class OtpVerificationDto {
  private String username;
  private String firstName;
  private String lastName;
  private String email;
  private Role role;
  private String otp;
}
