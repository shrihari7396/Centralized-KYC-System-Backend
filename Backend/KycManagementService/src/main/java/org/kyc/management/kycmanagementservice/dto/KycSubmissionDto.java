package org.kyc.management.kycmanagementservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycSubmissionDto {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Identity details are required")
    @Valid
    private KycIdentityDto identity;

    @NotEmpty(message = "At least one document is required")
    @Valid
    private List<KycDocumentDto> documents;
}
