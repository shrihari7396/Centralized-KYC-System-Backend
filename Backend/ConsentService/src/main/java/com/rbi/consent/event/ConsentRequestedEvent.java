package com.rbi.consent.event;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentRequestedEvent {

  private String userId;
  private Long consentId;
  private LocalDateTime timestamp;
}
