package org.kyc.management.bankservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankKycSubmissionRequest {
    @NotNull(message = "User ID is required")
    private String userId;

    @NotNull(message = "Identity is required")
    @Valid
    private KycIdentityDto identity;

    @NotNull(message = "Documents are required")
    @Valid
    private List<KycDocumentDto> documents;
}
