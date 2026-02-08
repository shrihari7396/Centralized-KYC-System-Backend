package org.kyc.digilockerService.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class DocumentResponse {
    private List<VerifiedDocumentProof> documents;
}
