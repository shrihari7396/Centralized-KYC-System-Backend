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
    private String userId;
    private UUID bankId;
    private String jwtToken;
}
