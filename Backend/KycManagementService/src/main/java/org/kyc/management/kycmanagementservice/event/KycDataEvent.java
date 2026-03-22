package org.kyc.management.kycmanagementservice.event;

import lombok.*;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycDataEvent {
    private String userId;
    private String bankId;
    private String consentId;
    private Map<String, Object> kycData;
}
