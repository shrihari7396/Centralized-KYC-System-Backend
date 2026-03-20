package org.kyc.management.kycmanagementservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.kyc.management.kycmanagementservice.entity.Address;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DigilockerDataRequest {
    private String userId;
    private String aadhaarNo;
    private String pan;
    private String drivingLicense;
    private Address address;
}
