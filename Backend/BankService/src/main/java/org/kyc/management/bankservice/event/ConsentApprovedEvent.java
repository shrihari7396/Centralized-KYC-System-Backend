package org.kyc.management.bankservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentApprovedEvent {
    private UUID consentId;
    private Long userId; // Assuming BankService uses Long for userId as per entity
    private UUID bankId;
    private String jwtToken;
}
