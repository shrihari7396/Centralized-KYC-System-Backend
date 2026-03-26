package org.rbi.kycmanagementservice.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.rbi.kycmanagementservice.dto.DigilockerDataRequest;
import org.rbi.kycmanagementservice.dto.DocumentProofResponseDto;
import org.rbi.kycmanagementservice.dto.KycTokenResponseDto;
import org.rbi.kycmanagementservice.dto.UserProfileResponseDto;
import org.rbi.kycmanagementservice.dto.VerifiedIdentity;
import org.rbi.kycmanagementservice.entity.KycEntity;
import org.rbi.kycmanagementservice.entity.KycRecord;
import org.rbi.kycmanagementservice.repository.KycRecordRepository;
import org.rbi.kycmanagementservice.repository.KycRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class KycService {

    @Autowired private KycRepository kycRepository;

    @Autowired private KycRecordRepository kycRecordRepository;

    @Autowired private KycTokenRedisService kycTokenRedisService;

    @Autowired private JwtParserUtil jwtParserUtil;

    @Autowired private TokenBlacklistService tokenBlacklistService;

    /**
     * Stores incoming data from Digilocker with a PENDING verification status. Overwrites existing
     * records for the same User ID.
     */
    public KycEntity storeDigilockerData(DigilockerDataRequest request) {
        KycEntity entity =
                KycEntity.builder()
                        .userId(request.getUserId())
                        .aadhaarNo(request.getAadhaarNo())
                        .pan(request.getPan())
                        .drivingLicense(request.getDrivingLicense())
                        .address(request.getAddress())
                        .status("PENDING") // Default status until verified
                        .build();

        return kycRepository.save(entity);
    }

    /** Validates Bank request and returns matching data only if the profile is VERIFIED. */
    public Map<String, Object> getVerifiedDataForBank(String token) {
        if (jwtParserUtil.isTokenExpired(token)) {
            throw new RuntimeException("Invalid JWT: Token has expired.");
        }

        String jti = jwtParserUtil.extractJti(token);
        if (jti != null && tokenBlacklistService.isBlacklisted(jti)) {
            throw new RuntimeException("Invalid JWT: Token is blacklisted (already used).");
        }

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
            throw new RuntimeException(
                    "KYC Data is not verified. Current status: " + entity.getStatus());
        }

        List<String> requestedFields = jwtParserUtil.extractRequestedFields(token);

        Map<String, Object> responseFields = filterEntityFields(entity, requestedFields);

        // Blacklist token after successful processing
        if (jti != null) {
            long remainingSeconds = jwtParserUtil.getExpirationRemainingSeconds(token);
            tokenBlacklistService.blacklistToken(
                    jti, remainingSeconds > 0 ? remainingSeconds : 3600); // fallback 1h
        }

        return responseFields;
    }

    /** Dynamically filters the object to return only specific requested fields (if any). */
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
        if (requestedFields.contains("aadhaarNo"))
            responseMap.put("aadhaarNo", entity.getAadhaarNo());
        if (requestedFields.contains("pan")) responseMap.put("pan", entity.getPan());
        if (requestedFields.contains("drivingLicense"))
            responseMap.put("drivingLicense", entity.getDrivingLicense());

        // Return full address object if any address references are requested
        if (requestedFields.contains("address")
                || requestedFields.contains("street")
                || requestedFields.contains("city")) {
            responseMap.put("address", entity.getAddress());
        }

        responseMap.put("status", entity.getStatus());

        return responseMap;
    }

    public UserProfileResponseDto getUserProfile(String userId) {
        KycEntity kycEntity =
                kycRepository
                        .findById(userId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "KYC Entity not found for user: " + userId));

        VerifiedIdentity identity = new VerifiedIdentity();
        identity.setGender("Not Available"); // Not available in current schema

        if (kycEntity.getAadhaarNo() != null) {
            Optional<KycRecord> recordOpt =
                    kycRecordRepository.findByAadharNumber(kycEntity.getAadhaarNo());
            if (recordOpt.isPresent()) {
                KycRecord record = recordOpt.get();
                String fullName =
                        (record.getFirstName() != null ? record.getFirstName() : "")
                                + " "
                                + (record.getMiddleName() != null ? record.getMiddleName() : "")
                                + " "
                                + (record.getLastName() != null ? record.getLastName() : "");
                identity.setFullName(fullName.replaceAll("\\s+", " ").trim());
                identity.setDob(record.getDob());
            }
        }

        return UserProfileResponseDto.builder()
                .status(kycEntity.getStatus())
                .identity(identity)
                .address(kycEntity.getAddress())
                .build();
    }

    public List<DocumentProofResponseDto> getDocumentProofs(String userId) {
        KycEntity kycEntity =
                kycRepository
                        .findById(userId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "KYC Entity not found for user: " + userId));

        LocalDateTime verifiedAt = null;
        if (kycEntity.getAadhaarNo() != null) {
            Optional<KycRecord> recordOpt =
                    kycRecordRepository.findByAadharNumber(kycEntity.getAadhaarNo());
            if (recordOpt.isPresent()) {
                verifiedAt = recordOpt.get().getUpdatedAt();
            }
        }
        if (verifiedAt == null) verifiedAt = LocalDateTime.now(); // Fallback

        List<DocumentProofResponseDto> proofs = new ArrayList<>();

        if (kycEntity.getAadhaarNo() != null) {
            String maskedId = maskId(kycEntity.getAadhaarNo());
            proofs.add(new DocumentProofResponseDto("AADHAAR", "UIDAI", maskedId, verifiedAt));
        }

        if (kycEntity.getPan() != null) {
            String maskedId = maskId(kycEntity.getPan());
            proofs.add(new DocumentProofResponseDto("PAN", "ITD", maskedId, verifiedAt));
        }

        if (kycEntity.getDrivingLicense() != null) {
            String maskedId = maskId(kycEntity.getDrivingLicense());
            proofs.add(
                    new DocumentProofResponseDto("DRIVING_LICENSE", "RTO", maskedId, verifiedAt));
        }

        return proofs;
    }

    private String maskId(String id) {
        if (id == null || id.length() <= 4) return id;
        String last4 = id.substring(id.length() - 4);
        return "XXXX-XXXX-" + last4;
    }

    public KycTokenResponseDto getKycToken(String userId) {
        // Attempt to fetch existing token from Redis
        Optional<KycTokenResponseDto> existingToken = kycTokenRedisService.getToken(userId);
        if (existingToken.isPresent()) {
            return existingToken.get();
        }

        // Generate a new token if not present
        KycTokenResponseDto newToken =
                KycTokenResponseDto.builder()
                        .tokenId(UUID.randomUUID().toString())
                        .allowedData(Arrays.asList("NAME", "DOB", "ADDRESS"))
                        .issuedAt(LocalDateTime.now())
                        .expiresAt(LocalDateTime.now().plusYears(1))
                        .status("ACTIVE")
                        .build();

        kycTokenRedisService.saveToken(userId, newToken);
        return newToken;
    }

    public void revokeKycToken(String userId) {
        kycTokenRedisService.revokeToken(userId);
    }
}
