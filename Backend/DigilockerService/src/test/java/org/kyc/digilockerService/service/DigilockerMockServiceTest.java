package org.kyc.digilockerService.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kyc.digilockerService.dto.*;
import org.kyc.digilockerService.model.DocumentType;
import org.kyc.digilockerService.model.IdentityVerification;
import org.kyc.digilockerService.repository.InMemoryVerificationRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DigilockerMockServiceTest {

    @Mock
    private InMemoryVerificationRepository repository;

    @InjectMocks
    private DigilockerMockService service;

    private OtpGenerateRequest generateRequest;
    private OtpVerifyRequest verifyRequest;

    @BeforeEach
    void setUp() {
        generateRequest = new OtpGenerateRequest();
        generateRequest.setDocumentType(DocumentType.PAN);
        generateRequest.setDocumentNumber("ABCDE1234F");

        verifyRequest = new OtpVerifyRequest();
        verifyRequest.setRequestId("test-request-id");
        verifyRequest.setOtp("123456");
        verifyRequest.setDocumentNumber("ABCDE1234F");
    }

    @Test
    void testGenerateOtp_Success() {
        doNothing().when(repository).save(any(IdentityVerification.class));

        OtpGenerateResponse response = service.generateOtp(generateRequest);

        assertNotNull(response);
        assertNotNull(response.getRequestId());
        assertEquals("OTP sent successfully", response.getMessage());
        verify(repository, times(1)).save(any(IdentityVerification.class));
    }

    @Test
    void testVerifyOtp_Success() {
        IdentityVerification mockVerification = IdentityVerification.builder()
                .requestId("test-request-id")
                .otp("123456")
                .otpExpiry(LocalDateTime.now().plusMinutes(5))
                .documentNumber("ABCDE1234F")
                .build();

        when(repository.findByRequestId("test-request-id")).thenReturn(Optional.of(mockVerification));
        doNothing().when(repository).save(any(IdentityVerification.class));

        OtpVerifyResponse response = service.verifyOtp(verifyRequest);

        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getVerificationToken());
    }

    @Test
    void testVerifyOtp_InvalidOtp() {
        IdentityVerification mockVerification = IdentityVerification.builder()
                .requestId("test-request-id")
                .otp("654321") // Different OTP
                .otpExpiry(LocalDateTime.now().plusMinutes(5))
                .documentNumber("ABCDE1234F")
                .build();

        when(repository.findByRequestId("test-request-id")).thenReturn(Optional.of(mockVerification));

        OtpVerifyResponse response = service.verifyOtp(verifyRequest);

        assertEquals("FAILED", response.getStatus());
        assertEquals("INVALID_OTP", response.getReason());
    }

    @Test
    void testVerifyOtp_Expired() {
        IdentityVerification mockVerification = IdentityVerification.builder()
                .requestId("test-request-id")
                .otp("123456")
                .otpExpiry(LocalDateTime.now().minusMinutes(1)) // Expired
                .documentNumber("ABCDE1234F")
                .build();

        when(repository.findByRequestId("test-request-id")).thenReturn(Optional.of(mockVerification));

        OtpVerifyResponse response = service.verifyOtp(verifyRequest);

        assertEquals("FAILED", response.getStatus());
        assertEquals("OTP_EXPIRED", response.getReason());
    }

    @Test
    void testVerifyOtp_InvalidRequestId() {
        when(repository.findByRequestId("test-request-id")).thenReturn(Optional.empty());

        OtpVerifyResponse response = service.verifyOtp(verifyRequest);

        assertEquals("FAILED", response.getStatus());
        assertEquals("INVALID_REQUEST_ID", response.getReason());
    }

    @Test
    void testFetchDocument_Success() {
        String token = "valid-token";
        IdentityVerification mockVerification = IdentityVerification.builder()
                .verificationToken(token)
                .tokenExpiry(LocalDateTime.now().plusHours(1))
                .documentType(DocumentType.PAN)
                .documentNumber("ABCDE1234F")
                .build();

        when(repository.findByVerificationToken(token)).thenReturn(Optional.of(mockVerification));

        DocumentResponse response = service.fetchDocument(token, DocumentType.PAN, "ABCDE1234F");

        assertNotNull(response);
        assertEquals("XXXX234F", response.getDocumentNumber());
        assertNotNull(response.getName());
        assertNotNull(response.getDob());
    }

    @Test
    void testFetchDocument_InvalidToken() {
        when(repository.findByVerificationToken("invalid-token")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.fetchDocument("invalid-token", DocumentType.PAN, "ABCDE1234F"));
    }
}
