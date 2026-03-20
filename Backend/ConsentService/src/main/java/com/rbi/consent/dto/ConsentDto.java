package com.rbi.consent.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentDto {

  private Long consentId;
  private String userId;
  private String dataUsage;
  private String purpose;
  private String status; // REQUESTED/GIVEN/WITHDRAWN
  private LocalDateTime grantedAt;
}
