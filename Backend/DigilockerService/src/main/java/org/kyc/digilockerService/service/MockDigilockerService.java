package org.kyc.digilockerService.service;

import org.kyc.digilockerService.model.ConsentRequest;
import org.kyc.digilockerService.model.ConsentResponse;
import org.kyc.digilockerService.model.DocumentResponse;
import org.kyc.digilockerService.model.VerifiedIdentity;

public interface MockDigilockerService {
    ConsentResponse grantConsent(ConsentRequest request);

    VerifiedIdentity getUserProfile(String digilockerUserId);

    DocumentResponse getDocuments(String digilockerUserId);
}
