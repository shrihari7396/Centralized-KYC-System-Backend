package org.kyc.management.bankservice.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
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
    private UUID bankId;

    @NotNull(message = "Requested documents are required")
    private List<String> requestedDocuments;

    @NotNull(message = "User email is required")
    private String email;
}
