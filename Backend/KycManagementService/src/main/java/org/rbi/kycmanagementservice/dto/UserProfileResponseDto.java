package org.rbi.kycmanagementservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rbi.kycmanagementservice.entity.Address;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserProfileResponseDto {
    private String status;
    private VerifiedIdentity identity;
    private Address address;
}
