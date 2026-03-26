package org.rbi.kycmanagementservice.event;

import java.util.Map;
import lombok.*;

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
