package org.kyc.management.kycmanagementservice.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KycTokenResponseDto {
    private String tokenId;
    private List<String> allowedData;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private String status;
}
