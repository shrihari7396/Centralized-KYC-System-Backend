package org.kyc.digilockerService.util;

import org.kyc.digilockerService.model.MockDocument;
import org.kyc.digilockerService.model.MockUser;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

    public static MockUser generateMockUser(String userId) {
        String name = NAMES[random.nextInt(NAMES.length)];
        String gender = GENDERS[random.nextInt(GENDERS.length)];
        LocalDate dob = LocalDate.of(1980 + random.nextInt(20), 1 + random.nextInt(12), 1 + random.nextInt(28));

        return MockUser.builder()
                .digilockerUserId(userId)
                .name(name)
                .dob(dob.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .gender(gender)
                .aadhaar("XXXX-XXXX-" + (1000 + random.nextInt(9000)))
                .mobile("9" + (100000000 + random.nextInt(900000000)))
                .email(name.toLowerCase().replace(" ", ".") + "@testmail.com")
                .build();
    }

    public static List<MockDocument> generateMockDocuments() {
        List<MockDocument> documents = new ArrayList<>();

        // Aadhaar
        String aadhaarContent = Base64.getEncoder().encodeToString("Dummy Aadhaar XML Content".getBytes());
        documents.add(MockDocument.builder()
                .documentId(UUID.randomUUID().toString())
                .type("AADHAAR")
                .issuedBy("UIDAI")
                .issueDate("2015-06-01")
                .content(aadhaarContent)
                .documentHash(generateSha256(aadhaarContent))
                .build());

        // PAN
        String panContent = Base64.getEncoder().encodeToString("Dummy PAN Card Content".getBytes());
        documents.add(MockDocument.builder()
                .documentId(UUID.randomUUID().toString())
                .type("PAN")
                .issuedBy("Income Tax Department")
                .issueDate("2017-03-12")
                .content(panContent)
                .documentHash(generateSha256(panContent))
                .build());

        // Driving License
        String dlContent = Base64.getEncoder().encodeToString("Dummy Driving License Content".getBytes());
        documents.add(MockDocument.builder()
                .documentId(UUID.randomUUID().toString())
                .type("DRIVING_LICENSE")
                .issuedBy("Ministry of Road Transport and Highways")
                .issueDate("2019-11-20")
                .content(dlContent)
                .documentHash(generateSha256(dlContent))
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
