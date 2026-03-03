package org.kyc.management.kycmanagementservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycDecisionDto {

    @NotNull(message = "Case ID is required")
    private Long caseId;

    @NotNull(message = "Decision of approval is required")
    private Boolean approved;

    @NotNull(message = "Reviewer ID is required")
    private Long reviewerId;

    private String comments;
}
