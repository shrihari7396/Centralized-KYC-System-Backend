package org.kyc.emailservice.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerifyResponseDto {

  private Boolean verified;
  private LocalDateTime timestamp;
  private String message;
}
