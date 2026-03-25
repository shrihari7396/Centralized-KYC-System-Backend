package org.kyc.management.bankservice.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.kyc.management.bankservice.entity.DocumentType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkKycRequest {
    private List<String> emails;
    private List<DocumentType> requestedDocuments;
    private String bankId;
}
