package org.rbi.kycmanagementservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rbi.kycmanagementservice.entity.Address;

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
