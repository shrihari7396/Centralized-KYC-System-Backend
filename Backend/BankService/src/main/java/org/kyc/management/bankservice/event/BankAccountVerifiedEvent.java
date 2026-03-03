package org.kyc.management.bankservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountVerifiedEvent {

    private Long userId;
    private String accountNumber;
    private Integer bankId;
    private Boolean isValid;
    private LocalDateTime timestamp;
}
