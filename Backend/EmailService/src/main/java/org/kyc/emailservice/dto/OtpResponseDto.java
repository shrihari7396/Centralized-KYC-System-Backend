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
public class OtpResponseDto {

  private Long otpRequestId;
  private String status;
  private LocalDateTime expiresAt;
}
