package org.kyc.management.kycmanagementservice.event;

import java.util.List;
import lombok.*;
import org.kyc.management.kycmanagementservice.dto.DocumentType;
import org.kyc.management.kycmanagementservice.enums.ConsentStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentResponseEvent {
    private String consentId;
    private String userId;
    private String bankId;
    private ConsentStatus status;
    private List<DocumentType> requestedDocuments;
}
