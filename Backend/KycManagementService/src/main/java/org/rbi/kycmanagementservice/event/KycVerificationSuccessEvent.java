package org.rbi.kycmanagementservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KycVerificationSuccessEvent {
    private String email;
    private String mobileNumber;
    private String status;
    private String fullName;
}
