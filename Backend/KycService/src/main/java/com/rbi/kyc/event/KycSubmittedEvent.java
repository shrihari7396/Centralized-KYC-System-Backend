package com.rbi.kyc.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycSubmittedEvent {

    private Long userId;
    private Long kycRequestId;
    private String status;
    private LocalDateTime submittedAt;
}
