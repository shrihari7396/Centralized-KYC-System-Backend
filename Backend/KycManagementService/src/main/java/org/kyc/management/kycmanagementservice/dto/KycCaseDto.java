package org.kyc.management.kycmanagementservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycCaseDto {

    private Long caseId;
    private Long userId;
    private String status;
    private LocalDateTime initiatedAt;
    private LocalDateTime completedAt;
}
