package org.kyc.digilockerService.service;

import lombok.RequiredArgsConstructor;
import org.kyc.digilockerService.audit.AuditPublisher;
import org.kyc.digilockerService.model.*;
import org.kyc.digilockerService.util.DataGeneratorUtil;
import org.springframework.stereotype.Service;

import java.util.List;

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
    public MockUser getUserProfile(String digilockerUserId) {
        MockUser user = DataGeneratorUtil.generateMockUser(digilockerUserId);

        auditPublisher.publishEvent(AuditEvent.builder()
                .eventType("USER_PROFILE_FETCH")
                .userId(digilockerUserId)
                .timestamp(DataGeneratorUtil.getCurrentTimestamp())
                .source("digilocker-mock-service")
                .build());

        return user;
    }

    @Override
    public DocumentResponse getDocuments(String digilockerUserId) {
        List<MockDocument> documents = DataGeneratorUtil.generateMockDocuments();

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
}
