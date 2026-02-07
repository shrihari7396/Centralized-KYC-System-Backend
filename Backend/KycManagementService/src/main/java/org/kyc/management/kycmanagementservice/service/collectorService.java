package org.kyc.management.kycmanagementservice.service;

import org.kyc.management.kycmanagementservice.client.DigilockerProfileService;
import org.kyc.management.kycmanagementservice.client.DocumentFetchService;
import org.kyc.management.kycmanagementservice.dto.DocumentResponse;
import org.kyc.management.kycmanagementservice.entity.MockUser;
import org.kyc.management.kycmanagementservice.repository.DocumentRepository;
import org.kyc.management.kycmanagementservice.repository.ProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class collectorService {
    @Autowired
    private DigilockerProfileService digilockerProfileService;
    @Autowired
    private DocumentFetchService documentFetchService;

    @Autowired
    private DocumentRepository documentRepository;
    @Autowired
    private ProfileRepository
    profileRepository;

    MockUser fetch_profile(String digilockerUserId){
       return  digilockerProfileService.getUserProfile(digilockerUserId);
    }

    DocumentResponse getdocument(String digilockerUserId){
        return documentFetchService.getDocuments(digilockerUserId);
    }
    MockUser save_profile(String digilockerUserId){
        return  profileRepository.findById(digilockerUserId).orElse(null);
    }

    DocumentResponse saveDocument(String digilockerUserId){
        return documentRepository.findById(digilockerUserId).orElse(null);
    }
}
