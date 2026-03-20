package org.kyc.management.bankservice.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycSubmissionDto {
    private String userId;
    private KycIdentityDto identity;
    private List<KycDocumentDto> documents;
}
