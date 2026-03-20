package org.kyc.management.bankservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankKycInitiateRequest {
    @NotNull(message = "User ID is required")
    private String userId;

    @NotNull(message = "Bank ID is required")
    private java.util.UUID bankId;

    @NotNull(message = "Requested documents are required")
    private java.util.List<String> requestedDocuments;
}
