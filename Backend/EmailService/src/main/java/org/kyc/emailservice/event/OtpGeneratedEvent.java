package org.kyc.emailservice.event;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpGeneratedEvent {

  private String userId;
  private Long otpRequestId;
  private LocalDateTime createdAt;
  private LocalDateTime expiresAt;
}
