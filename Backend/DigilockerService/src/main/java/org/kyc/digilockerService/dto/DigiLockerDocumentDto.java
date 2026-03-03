package org.kyc.digilockerService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DigiLockerDocumentDto {

    private String documentId;
    private String documentType;
    private String issuer;
    private LocalDateTime issuedAt;
    private LocalDateTime validUntil;
    private String contentUrl;
}
