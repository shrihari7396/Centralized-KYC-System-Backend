package org.kyc.management.kycmanagementservice.service;

import org.kyc.management.kycmanagementservice.client.DigilockerProfileService;
import org.kyc.management.kycmanagementservice.client.DocumentFetchService;
import org.kyc.management.kycmanagementservice.dto.DocumentResponse;
import org.kyc.management.kycmanagementservice.entity.MockUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class collectorService {
    @Autowired
    private DigilockerProfileService digilockerProfileService;
    @Autowired
    private DocumentFetchService documentFetchService;

    MockUser fetch_profile(String digilockerUserId){
       return  digilockerProfileService.getUserProfile(digilockerUserId);
    }

    DocumentResponse getdocument(String digilockerUserId){
        return documentFetchService.getDocuments(digilockerUserId);
    }



}
