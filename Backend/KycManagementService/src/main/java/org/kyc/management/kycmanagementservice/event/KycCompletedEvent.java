package org.kyc.management.kycmanagementservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycCompletedEvent {

    private Long caseId;
    private Long userId;
    private String finalStatus;
    private LocalDateTime timestamp;
}
