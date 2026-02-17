package org.kyc.digilockerService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kyc.digilockerService.dto.*;
import org.kyc.digilockerService.model.DocumentType;
import org.kyc.digilockerService.model.IdentityVerification;
import org.kyc.digilockerService.repository.InMemoryVerificationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DigilockerMockService {

    private final InMemoryVerificationRepository repository;

    // Simulate API Failure (10% chance)
    private void simulateRandomFailure() {
        if (Math.random() < 0.1) {
            throw new RuntimeException("Simulated External API Failure");
        }
    }

    public OtpGenerateResponse generateOtp(OtpGenerateRequest request) {
        simulateRandomFailure();

        String requestId = UUID.randomUUID().toString();
        // Generate 6 digit OTP
        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);

        // Log OTP as per requirement
        log.info("DIGILOCKER MOCK OTP for Request [{}]: {}", requestId, otp);

        IdentityVerification verification = IdentityVerification.builder()
                .requestId(requestId)
                .documentType(request.getDocumentType())
                .documentNumber(request.getDocumentNumber())
                .otp(otp)
                .otpExpiry(LocalDateTime.now().plusMinutes(5))
                .build();

        repository.save(verification);

        return OtpGenerateResponse.builder()
                .requestId(requestId)
                .message("OTP sent successfully")
                .build();
    }

    public OtpVerifyResponse verifyOtp(OtpVerifyRequest request) {
        simulateRandomFailure();

        IdentityVerification verification = repository.findByRequestId(request.getRequestId())
                .orElse(null);

        if (verification == null) {
            return OtpVerifyResponse.builder()
                    .status("FAILED")
                    .reason("INVALID_REQUEST_ID")
                    .build();
        }

        if (LocalDateTime.now().isAfter(verification.getOtpExpiry())) {
            return OtpVerifyResponse.builder()
                    .status("FAILED")
                    .reason("OTP_EXPIRED")
                    .build();
        }

        if (!verification.getOtp().equals(request.getOtp())) {
            return OtpVerifyResponse.builder()
                    .status("FAILED")
                    .reason("INVALID_OTP")
                    .build();
        }

        if (!verification.getDocumentNumber().equalsIgnoreCase(request.getDocumentNumber())) {
            return OtpVerifyResponse.builder()
                    .status("FAILED")
                    .reason("DOCUMENT_MISMATCH")
                    .build();
        }

        String token = UUID.randomUUID().toString();
        verification.setVerificationToken(token);
        verification.setTokenExpiry(LocalDateTime.now().plusHours(1));
        repository.save(verification);

        return OtpVerifyResponse.builder()
                .status("SUCCESS")
                .verificationToken(token)
                .build();
    }

    public DocumentResponse fetchDocument(String verificationToken, DocumentType documentType, String documentNumber) {
        simulateRandomFailure();

        IdentityVerification verification = repository.findByVerificationToken(verificationToken)
                .orElseThrow(() -> new RuntimeException("Invalid Verification Token"));

        if (LocalDateTime.now().isAfter(verification.getTokenExpiry())) {
            throw new RuntimeException("Verification Token Expired");
        }

        // Additional checks if needed (match expected document type/number with what
        // was verified)
        if (verification.getDocumentType() != documentType
                || !verification.getDocumentNumber().equalsIgnoreCase(documentNumber)) {
            throw new RuntimeException("Token does not match requested document");
        }

        // Generate Dummy Data based on UUID seed (verificationToken hash code)
        // seed allows deterministic "random" data for the same session if needed,
        // or just truly random. Prompt says "Generate dummy verified data dynamically
        // using UUID seed"
        long seed = verificationToken.hashCode();

        // Simple deterministic dummy generation
        String name = "User_" + Math.abs(seed % 1000);
        String gender = (seed % 2 == 0) ? "M" : "F";
        String dob = LocalDate.of(1980 + (int) (Math.abs(seed) % 40), 1 + (int) (Math.abs(seed) % 12),
                1 + (int) (Math.abs(seed) % 28)).toString();

        return DocumentResponse.builder()
                .name(name)
                .dob(dob)
                .gender(gender)
                .address("Generated Address " + Math.abs(seed % 100))
                .documentNumber(maskDocumentNumber(documentNumber))
                .build();
    }

    private String maskDocumentNumber(String docNum) {
        if (docNum == null || docNum.length() < 4)
            return "****";
        return "XXXX" + docNum.substring(docNum.length() - 4);
    }
}
