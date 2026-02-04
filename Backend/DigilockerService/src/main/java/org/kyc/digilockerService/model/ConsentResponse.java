package org.kyc.digilockerService.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConsentResponse {
    private String consentId;
    private String status;
    private String consentTimestamp;
}
