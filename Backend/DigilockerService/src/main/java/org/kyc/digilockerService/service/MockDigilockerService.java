package org.kyc.digilockerService.service;

import org.kyc.digilockerService.model.ConsentRequest;
import org.kyc.digilockerService.model.ConsentResponse;
import org.kyc.digilockerService.model.DocumentResponse;
import org.kyc.digilockerService.model.MockUser;

public interface MockDigilockerService {
    ConsentResponse grantConsent(ConsentRequest request);

    MockUser getUserProfile(String digilockerUserId);

    DocumentResponse getDocuments(String digilockerUserId);
}
