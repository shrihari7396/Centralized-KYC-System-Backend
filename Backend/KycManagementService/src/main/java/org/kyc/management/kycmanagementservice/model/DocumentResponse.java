package org.kyc.management.kycmanagementservice.model;

import lombok.Builder;
import lombok.Data;
import org.kyc.digilockerService.model.VerifiedDocumentProof;

import java.util.List;

@Data
@Builder
public class DocumentResponse {
    private List<VerifiedDocumentProof> documents;
}
