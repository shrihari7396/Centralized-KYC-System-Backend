package org.kyc.digilockerService.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MockDocument {
    private String documentId;
    private String type;
    private String issuedBy;
    private String issueDate;
    private String content; // Base64 or JSON content
    private String documentHash; // SHA-256 hash for tokenisation
}
