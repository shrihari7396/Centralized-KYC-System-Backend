package org.kyc.management.bankservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankKycInitiateRequest {
    @NotNull(message = "User ID is required")
    private Long userId;
}
