package org.kyc.management.kycmanagementservice.dto;

import lombok.Builder;
import lombok.Data;
import org.kyc.digilockerService.model.VerifiedDocumentProof;

import java.util.List;

@Data
@Builder
public class DocumentResponse {
    private List<VerifiedDocumentProof> documents;
}
