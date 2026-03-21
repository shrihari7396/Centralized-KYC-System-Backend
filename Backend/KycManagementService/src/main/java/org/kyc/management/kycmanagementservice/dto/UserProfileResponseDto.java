package org.kyc.management.kycmanagementservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.kyc.management.kycmanagementservice.entity.Address;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserProfileResponseDto {
    private String status;
    private VerifiedIdentity identity;
    private Address address;
}
