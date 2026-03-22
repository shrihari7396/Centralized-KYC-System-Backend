package org.kyc.management.bankservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kyc.management.bankservice.client.KycFeignClient;
import org.kyc.management.bankservice.client.UserFeignClient;
import org.kyc.management.bankservice.dto.*;
import org.kyc.management.bankservice.entity.BankCustomer;
import org.kyc.management.bankservice.entity.KycStatus;
import org.kyc.management.bankservice.event.ConsentRequestEvent;
import org.kyc.management.bankservice.logging.KafkaLogProducer;
import org.kyc.management.bankservice.repository.BankCustomerRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class KycOrchestrationServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private KycFeignClient kycClient;

    @Mock
    private BankCustomerRepository bankCustomerRepository;

    @Mock
    private KafkaLogProducer kafkaLogProducer;

    @Mock
    private UserFeignClient userFeignClient;

    @InjectMocks
    private KycOrchestrationService kycOrchestrationService;

    private BankKycInitiateRequest initiateRequest;
    private BankKycSubmissionRequest submissionRequest;
    private UserDto testUserDto;
    private BankCustomer testCustomer;

    @BeforeEach
    void setUp() {
        UUID bankId = UUID.randomUUID();
        List<String> requestedDocuments = List.of("passport", "utility_bill");

        initiateRequest = new BankKycInitiateRequest(
                "user-123",
                bankId,
                requestedDocuments,
                "test@example.com"
        );

        submissionRequest = BankKycSubmissionRequest.builder()
                .userId("user-123")
                .identity(KycIdentityDto.builder()
                        .firstName("John")
                        .lastName("Doe")
                        .dateOfBirth("1990-01-01")
                        .build())
                .documents(List.of(KycDocumentDto.builder()
                        .type("passport")
                        .url("http://example.com/passport.jpg")
                        .build()))
                .build();

        testUserDto = new UserDto("global-user-123", "John", "Doe", "test@example.com", "1234567890", Arrays.asList("CUSTOMER"), "ACTIVE");

        testCustomer = BankCustomer.builder()
                .id("customer-123")
                .userId("global-user-123")
                .bankId(bankId.toString())
                .kycStatus(KycStatus.CONSENT_PENDING)
                .build();
    }

    @Test
    void initiateKyc_ShouldCreateNewCustomer_WhenCustomerDoesNotExist() {
        // Arrange
        when(userFeignClient.getUserDetails(initiateRequest.getUserId())).thenReturn(testUserDto);
        when(bankCustomerRepository.findByUserIdAndBankId(testUserDto.getUserId(), initiateRequest.getBankId().toString()))
                .thenReturn(Optional.empty());
        when(bankCustomerRepository.save(any(BankCustomer.class))).thenReturn(testCustomer);

        // Act
        BankCustomer result = kycOrchestrationService.initiateKyc(initiateRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testUserDto.getUserId(), result.getUserId());
        assertEquals(initiateRequest.getBankId().toString(), result.getBankId());
        assertEquals(KycStatus.CONSENT_PENDING, result.getKycStatus());
        verify(kafkaTemplate).send(eq("consent-request-topic"), any(ConsentRequestEvent.class));
        verify(kafkaLogProducer).logInfo(eq("KYC_INITIATED"), any());
    }

    @Test
    void initiateKyc_ShouldUpdateExistingCustomer_WhenCustomerExists() {
        // Arrange
        when(userFeignClient.getUserDetails(initiateRequest.getUserId())).thenReturn(testUserDto);
        when(bankCustomerRepository.findByUserIdAndBankId(testUserDto.getUserId(), initiateRequest.getBankId().toString()))
                .thenReturn(Optional.of(testCustomer));
        when(bankCustomerRepository.save(any(BankCustomer.class))).thenReturn(testCustomer);

        // Act
        BankCustomer result = kycOrchestrationService.initiateKyc(initiateRequest);

        // Assert
        assertNotNull(result);
        assertEquals(KycStatus.CONSENT_PENDING, result.getKycStatus());
        verify(kafkaTemplate).send(eq("consent-request-topic"), any(ConsentRequestEvent.class));
        verify(kafkaLogProducer).logInfo(eq("KYC_INITIATED"), any());
    }

    @Test
    void submitKycData_ShouldUpdateStatusToVerified_WhenKycVerified() {
        // Arrange
        KycResponseDto kycResponse = new KycResponseDto();
        kycResponse.setStatus("VERIFIED");
        kycResponse.setMessage("KYC verification successful");

        when(kycClient.submitKyc(any(KycSubmissionDto.class), eq("Bearer API_TOKEN"))).thenReturn(kycResponse);
        when(bankCustomerRepository.findByUserId(submissionRequest.getUserId())).thenReturn(Optional.of(testCustomer));
        when(bankCustomerRepository.save(any(BankCustomer.class))).thenReturn(testCustomer);

        // Act
        KycResponseDto result = kycOrchestrationService.submitKycData(submissionRequest);

        // Assert
        assertEquals("VERIFIED", result.getStatus());
        verify(bankCustomerRepository).save(argThat(customer ->
                customer.getKycStatus() == KycStatus.VERIFIED && customer.getVerifiedAt() != null));
    }

    @Test
    void submitKycData_ShouldUpdateStatusToRejected_WhenKycRejected() {
        // Arrange
        KycResponseDto kycResponse = new KycResponseDto();
        kycResponse.setStatus("REJECTED");
        kycResponse.setMessage("KYC verification failed");

        when(kycClient.submitKyc(any(KycSubmissionDto.class), eq("Bearer API_TOKEN"))).thenReturn(kycResponse);
        when(bankCustomerRepository.findByUserId(submissionRequest.getUserId())).thenReturn(Optional.of(testCustomer));
        when(bankCustomerRepository.save(any(BankCustomer.class))).thenReturn(testCustomer);

        // Act
        KycResponseDto result = kycOrchestrationService.submitKycData(submissionRequest);

        // Assert
        assertEquals("REJECTED", result.getStatus());
        verify(bankCustomerRepository).save(argThat(customer ->
                customer.getKycStatus() == KycStatus.REJECTED));
    }

    @Test
    void submitKycData_ShouldThrowException_WhenCustomerNotFound() {
        // Arrange
        when(kycClient.submitKyc(any(KycSubmissionDto.class), eq("Bearer API_TOKEN"))).thenReturn(new KycResponseDto());
        when(bankCustomerRepository.findByUserId(submissionRequest.getUserId())).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> kycOrchestrationService.submitKycData(submissionRequest));
        assertEquals("Customer not found", exception.getMessage());
    }
}