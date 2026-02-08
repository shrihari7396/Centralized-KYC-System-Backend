package org.kyc.management.bankservice.dto;

import lombok.Builder;
import lombok.Data;
import org.kyc.management.kycmanagementservice.entity.MockDocument;

import java.util.List;

@Data
@Builder
public class DocumentResponse {
    private List<MockDocument> documents;
}
