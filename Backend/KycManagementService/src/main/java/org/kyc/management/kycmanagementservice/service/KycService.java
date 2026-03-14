package org.kyc.management.kycmanagementservice.service;

import org.kyc.management.kycmanagementservice.dto.DigilockerDataRequest;
import org.kyc.management.kycmanagementservice.entity.KycEntity;
import org.kyc.management.kycmanagementservice.repository.KycRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class KycService {

    @Autowired
    private KycRepository kycRepository;

    @Autowired
    private JwtParserUtil jwtParserUtil;

    /**
     * Stores incoming data from Digilocker with a PENDING verification status.
     * Overwrites existing records for the same User ID.
     */
    public KycEntity storeDigilockerData(DigilockerDataRequest request) {
        KycEntity entity = KycEntity.builder()
                .userId(request.getUserId())
                .aadhaarNo(request.getAadhaarNo())
                .pan(request.getPan())
                .drivingLicense(request.getDrivingLicense())
                .address(request.getAddress())
                .status("PENDING") // Default status until verified
                .build();

        return kycRepository.save(entity);
    }

    /**
     * Validates Bank request and returns matching data only if the profile is VERIFIED.
     */
    public Map<String, Object> getVerifiedDataForBank(String token) {
        String userId = jwtParserUtil.extractUserId(token);
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid JWT: User ID is missing.");
        }

        Optional<KycEntity> optionalEntity = kycRepository.findById(userId);

        if (optionalEntity.isEmpty()) {
            throw new RuntimeException("KYC Record not found for user: " + userId);
        }

        KycEntity entity = optionalEntity.get();

        if (!"VERIFIED".equalsIgnoreCase(entity.getStatus())) {
            throw new RuntimeException("KYC Data is not verified. Current status: " + entity.getStatus());
        }

        List<String> requestedFields = jwtParserUtil.extractRequestedFields(token);
        
        return filterEntityFields(entity, requestedFields);
    }

    /**
     * Dynamically filters the object to return only specific requested fields (if any).
     */
    private Map<String, Object> filterEntityFields(KycEntity entity, List<String> requestedFields) {
        Map<String, Object> responseMap = new HashMap<>();

        // If no specific fields requested, return standard core fields
        if (requestedFields == null || requestedFields.isEmpty()) {
            responseMap.put("userId", entity.getUserId());
            responseMap.put("aadhaarNo", entity.getAadhaarNo());
            responseMap.put("pan", entity.getPan());
            responseMap.put("drivingLicense", entity.getDrivingLicense());
            responseMap.put("address", entity.getAddress());
            responseMap.put("status", entity.getStatus());
            return responseMap;
        }

        // Add requested fields dynamically
        if (requestedFields.contains("userId")) responseMap.put("userId", entity.getUserId());
        if (requestedFields.contains("aadhaarNo")) responseMap.put("aadhaarNo", entity.getAadhaarNo());
        if (requestedFields.contains("pan")) responseMap.put("pan", entity.getPan());
        if (requestedFields.contains("drivingLicense")) responseMap.put("drivingLicense", entity.getDrivingLicense());
        
        // Return full address object if any address references are requested
        if (requestedFields.contains("address") || requestedFields.contains("street") || requestedFields.contains("city")) {
           responseMap.put("address", entity.getAddress()); 
        }

        responseMap.put("status", entity.getStatus());

        return responseMap;
    }
}
