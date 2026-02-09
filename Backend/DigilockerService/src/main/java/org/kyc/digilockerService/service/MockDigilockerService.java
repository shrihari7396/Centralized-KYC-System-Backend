package org.kyc.digilockerService.service;

import org.kyc.digilockerService.model.*;

public interface MockDigilockerService {

    ConsentResponse grantConsent(ConsentRequest request);

    VerifiedIdentity getUserProfile(String digilockerUserId);

    DocumentResponse getDocuments(String digilockerUserId);

    VerifiedAddress extractVerifiedAddress(String digilockerUserId);
}
