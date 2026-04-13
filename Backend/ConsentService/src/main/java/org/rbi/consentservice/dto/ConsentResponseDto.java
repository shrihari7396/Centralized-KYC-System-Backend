package org.rbi.consentservice.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rbi.consentservice.enums.ConsentStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentResponseDto {

    private String consentId;
    private String bankId;
    private ConsentStatus status;

    private LocalDateTime expiryAt;

    private BankDto bank;
}
