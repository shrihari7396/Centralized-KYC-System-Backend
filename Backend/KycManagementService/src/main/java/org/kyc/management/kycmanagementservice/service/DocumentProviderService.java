package org.kyc.management.kycmanagementservice.service;

import com.netflix.discovery.converters.Auto;
import org.kyc.management.kycmanagementservice.dto.DocumentResponse;
import org.kyc.management.kycmanagementservice.entity.MockUser;
import org.kyc.management.kycmanagementservice.repository.DocumentRepository;
import org.kyc.management.kycmanagementservice.repository.ProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
@Service
public class DocumentProviderService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private collectorService collectorService;

    public DocumentResponse getDocument(String userId) {
        DocumentResponse response = documentRepository.findAll(userId)
                .orElseGet(() -> collectorService.getdocument(userId));
        return  response;
    }

    public MockUser getProfile(String userId) {
        MockUser user =  profileRepository
                .findByAadhaar(userId);
        if(user==null){

        user  = collectorService.fetch_profile(userId);
        }
        return user;
    }
}
