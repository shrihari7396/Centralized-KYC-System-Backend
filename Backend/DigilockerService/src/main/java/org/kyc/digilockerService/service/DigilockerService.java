package org.kyc.digilockerService.service;

import org.kyc.digilockerService.model.KycStatus;

public interface DigilockerService {

    String getAuthorizationUrl(String state);

    String getAccessToken(String code, String state);

    KycStatus handleCallback(String code, String state);

    Object getUserDetails(String accessToken);

    Object getIssuedDocuments(String accessToken);

    byte[] getFileFromUri(String accessToken, String uri);

    Object pullDocument(String accessToken, Object pullRequest);
}
