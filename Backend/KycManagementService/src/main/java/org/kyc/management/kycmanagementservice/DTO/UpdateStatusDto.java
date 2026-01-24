package org.kyc.management.kycmanagementservice.DTO;

import lombok.Data;
import org.kyc.management.kycmanagementservice.entity.UserStatus;

@Data
public class UpdateStatusDto {
    private String email;
    private UserStatus status;
}
