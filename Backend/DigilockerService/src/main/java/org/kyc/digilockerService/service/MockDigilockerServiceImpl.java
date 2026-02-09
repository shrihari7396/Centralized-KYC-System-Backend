package org.kyc.digilockerService.service;

import lombok.RequiredArgsConstructor;
import org.kyc.digilockerService.audit.AuditPublisher;
import org.kyc.digilockerService.model.*;
import org.kyc.digilockerService.util.DataGeneratorUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MockDigilockerServiceImpl implements MockDigilockerService {

    private final AuditPublisher auditPublisher;

    @Override
    public ConsentResponse grantConsent(ConsentRequest request) {
        String consentId = DataGeneratorUtil.generateConsentId();
        String timestamp = DataGeneratorUtil.getCurrentTimestamp();

        // In a real scenario, we might create a user session here,
        // but for this mock, we just generate a User ID implicitly via the consent flow
        // if needed,
        // or just return success. The actual User ID will be generated/returned.
        // Wait, the prompt says "Generate UUID, Treat UUID as digilockerUserId".
        // But the consent response only returns consentId.
        // Let's assume the client will use consentId to fetch user data or the user
        // flow involves redirection.
        // However, looking at Requirement 1: "On each request or consent flow: Generate
        // a UUID".

        // Let's stick to the Consent Response structure requested: consentId, status,
        // timestamp.

        auditPublisher.publishEvent(AuditEvent.builder()
                .eventType("CONSENT_GRANTED")
                .userId("PENDING_GENERATION") // or associated with consentId
                .timestamp(timestamp)
                .source("digilocker-mock-service")
                .build());

        return ConsentResponse.builder()
                .consentId(consentId)
                .status("GRANTED")
                .consentTimestamp(timestamp)
                .build();
    }

    @Override
    public VerifiedIdentity getUserProfile(String digilockerUserId) {
        VerifiedIdentity identity = DataGeneratorUtil.generateVerifiedIdentity(digilockerUserId);

        auditPublisher.publishEvent(AuditEvent.builder()
                .eventType("USER_PROFILE_FETCH")
                .userId(digilockerUserId)
                .timestamp(DataGeneratorUtil.getCurrentTimestamp())
                .source("digilocker-mock-service")
                .build());

        return identity;
    }

    @Override
    public DocumentResponse getDocuments(String digilockerUserId) {
        List<VerifiedDocumentProof> documents = DataGeneratorUtil.generateMockDocuments(java.util.UUID.fromString(digilockerUserId));

        auditPublisher.publishEvent(AuditEvent.builder()
                .eventType("DOCUMENTS_FETCH")
                .userId(digilockerUserId)
                .timestamp(DataGeneratorUtil.getCurrentTimestamp())
                .source("digilocker-mock-service")
                .build());

        return DocumentResponse.builder()
                .documents(documents)
                .build();
    }
    @Override
    public VerifiedAddress extractVerifiedAddress(String digilockerUserId) {

        // Step 1: Fetch mock documents
        DocumentResponse response = getDocuments(digilockerUserId);

        // Step 2: Pick Aadhaar / DL (mock logic)
        VerifiedDocumentProof addressDoc = response.getDocuments().stream()
                .filter(doc ->
                        "AADHAAR".equalsIgnoreCase(doc.getDocumentType())
                                || "DRIVING_LICENSE".equalsIgnoreCase(doc.getDocumentType()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("No address proof found"));

        // Step 3: Extract verified address
        VerifiedAddress address = VerifiedAddress.builder()
                .addressId(UUID.randomUUID())
                .userId(UUID.fromString(digilockerUserId))
                .addressLine1("Flat 101, Sample Residency")
                .addressLine2("MG Road")
                .city("Pune")
                .district("Pune")
                .state("Maharashtra")
                .pincode("411001")
                .country("India")
                .addressSource(addressDoc.getDocumentType())
                .verifiedAt(java.time.LocalDateTime.now())
                .build();

        auditPublisher.publishEvent(AuditEvent.builder()
                .eventType("ADDRESS_VERIFIED")
                .userId(digilockerUserId)
                .timestamp(DataGeneratorUtil.getCurrentTimestamp())
                .source("digilocker-mock-service")
                .build());

        return address;
    }

}
