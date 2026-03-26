package org.rbi.kycmanagementservice.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DocumentProofResponseDto {
    private String documentType;
    private String issuer;
    private String maskedId;
    private LocalDateTime verifiedAt;
}
