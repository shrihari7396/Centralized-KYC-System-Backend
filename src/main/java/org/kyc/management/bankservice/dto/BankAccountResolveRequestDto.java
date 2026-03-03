package org.kyc.management.bankservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountResolveRequestDto {

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotBlank(message = "Bank code is required")
    private String bankCode;
}
