package org.kyc.management.bankservice.event;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
