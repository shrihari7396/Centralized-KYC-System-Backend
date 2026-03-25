package com.rbi.consent.dto;

import com.rbi.consent.enums.ConsentStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentResponseDto {

    private UUID consentId;
    private UUID bankId;
    private ConsentStatus status;

    private LocalDateTime expiryAt;

    private BankDto bank;
}
