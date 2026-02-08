package org.kyc.digilockerService.util;

import org.kyc.digilockerService.model.VerifiedDocumentProof;
import org.kyc.digilockerService.model.VerifiedIdentity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class DataGeneratorUtil {

    private static final Random random = new Random();
    private static final String[] NAMES = { "Ravi Kumar", "Priya Sharma", "Amit Singh", "Sneha Gupta", "Rahul Verma",
            "Anjali Mishra", "Vikram Patel", "Kavita Reddy" };
    private static final String[] GENDERS = { "M", "F" };

    public static String generateUuid() {
        return UUID.randomUUID().toString();
    }

    public static String generateConsentId() {
        return UUID.randomUUID().toString();
    }

    public static String getCurrentTimestamp() {
        return java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now());
    }

    public static VerifiedIdentity generateVerifiedIdentity(String userId) {
        String name = NAMES[random.nextInt(NAMES.length)];
        String gender = GENDERS[random.nextInt(GENDERS.length)];
        int year = 1980 + random.nextInt(20);
        int month = 1 + random.nextInt(12);
        int day = 1 + random.nextInt(28);
        String dob = String.format("%d-%02d-%02d", year, month, day);

        return VerifiedIdentity.builder()
                .identityId(UUID.randomUUID())
                .userId(UUID.fromString(userId))
                .fullName(name)
                .dateOfBirth(dob)
                .gender(gender)
                .verifiedAt(LocalDateTime.now())
                .verifiedBy("DIGILOCKER")
                .build();
    }

    public static List<VerifiedDocumentProof> generateMockDocuments(UUID userId) {
        List<VerifiedDocumentProof> documents = new ArrayList<>();

        // Aadhaar
        String aadhaarContent = "Dummy Aadhaar XML Content";
        documents.add(VerifiedDocumentProof.builder()
                .proofId(UUID.randomUUID())
                .userId(userId)
                .documentType("AADHAAR")
                .issuer("UIDAI")
                .maskedIdentifier("XXXX-XXXX-" + (1000 + random.nextInt(9000)))
                .documentHash(generateSha256(aadhaarContent))
                .digitallySigned(true)
                .verifiedAt(LocalDateTime.now())
                .build());

        // PAN
        String panContent = "Dummy PAN Card Content";
        documents.add(VerifiedDocumentProof.builder()
                .proofId(UUID.randomUUID())
                .userId(userId)
                .documentType("PAN")
                .issuer("ITD")
                .maskedIdentifier("ABCDE" + (1000 + random.nextInt(9000)) + "F")
                .documentHash(generateSha256(panContent))
                .digitallySigned(true)
                .verifiedAt(LocalDateTime.now())
                .build());

        // Driving License
        String dlContent = "Dummy Driving License Content";
        documents.add(VerifiedDocumentProof.builder()
                .proofId(UUID.randomUUID())
                .userId(userId)
                .documentType("DRIVING_LICENSE")
                .issuer("RTO")
                .maskedIdentifier("DL-" + (100000000 + random.nextInt(900000000)))
                .documentHash(generateSha256(dlContent))
                .digitallySigned(true)
                .verifiedAt(LocalDateTime.now())
                .build());

        return documents;
    }

    private static String generateSha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
