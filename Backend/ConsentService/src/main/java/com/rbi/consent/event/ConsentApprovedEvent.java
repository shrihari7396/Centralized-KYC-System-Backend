package com.rbi.consent.event;

import com.rbi.consent.enums.ConsentStatus;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentApprovedEvent {
    private UUID consentId;
    private String userId;
    private UUID bankId;
    private ConsentStatus status;
}
