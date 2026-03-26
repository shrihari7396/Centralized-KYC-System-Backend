package org.kyc.management.bankservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskUpdateRequest {
    private String userId;
    private String bankId;
    private String newRiskLevel;
    private String reason;
}
