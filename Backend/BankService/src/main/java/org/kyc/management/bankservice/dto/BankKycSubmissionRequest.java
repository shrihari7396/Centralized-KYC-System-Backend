package org.kyc.management.bankservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankKycSubmissionRequest {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Identity is required")
    @Valid
    private KycIdentityDto identity;

    @NotNull(message = "Documents are required")
    @Valid
    private List<KycDocumentDto> documents;
}
