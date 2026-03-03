package org.kyc.management.bankservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountResolveResponseDto {

    private String accountNumber;
    private String accountName;
    private Integer bankId;
    private Boolean isValid;
}
