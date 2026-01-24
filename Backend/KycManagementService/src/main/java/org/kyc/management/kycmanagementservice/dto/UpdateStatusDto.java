package org.kyc.management.kycmanagementservice.dto;

import lombok.Data;
import org.kyc.management.kycmanagementservice.entity.UserStatus;

@Data
public class UpdateStatusDto {
    private String email;
    private UserStatus status;
}
