package org.kyc.management.bankservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kyc.management.bankservice.client.UserFeignClient;
import org.kyc.management.bankservice.dto.*;
import org.kyc.management.bankservice.entity.BankCustomer;
import org.kyc.management.bankservice.entity.KycStatus;
import org.kyc.management.bankservice.event.ConsentRequestEvent;
import org.kyc.management.bankservice.logging.KafkaLogProducer;
import org.kyc.management.bankservice.repository.BankCustomerRepository;
import org.kyc.management.bankservice.repository.ConsentRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class KycOrchestrationServiceTest {

    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock private BankCustomerRepository bankCustomerRepository;

    @Mock private KafkaLogProducer kafkaLogProducer;

    @Mock private UserFeignClient userFeignClient;

    @InjectMocks private KycOrchestrationService kycOrchestrationService;
    @Mock private ConsentRepository consentRepository;

    private BankKycInitiateRequest initiateRequest;
    private UserDto testUserDto;
    private BankCustomer testCustomer;

    @BeforeEach
    void setUp() {
        UUID bankId = UUID.randomUUID();
        List<String> requestedDocuments = List.of("passport", "utility_bill");

        initiateRequest =
                new BankKycInitiateRequest(
                        "user-123", bankId, requestedDocuments, "test@example.com");

<<<<<<< Updated upstream
        submissionRequest = BankKycSubmissionRequest.builder()
                .userId("user-123")
                .identity(KycIdentityDto.builder()
                        .type("abc")
                        .number("1234567890")
                        .build())
                .documents(List.of(KycDocumentDto.builder()
                        .type("passport")
                        .content("http://example.com/passport.jpg")
                        .build()))
                .build();
=======
        testUserDto =
                new UserDto(
                        "global-user-123",
                        "John",
                        "Doe",
                        "test@example.com",
                        "1234567890",
                        Arrays.asList("CUSTOMER"),
                        "ACTIVE");
>>>>>>> Stashed changes

        testCustomer = new BankCustomer();
        testCustomer.setId("customer-123");
        testCustomer.setUserId("global-user-123");
        testCustomer.setBankId(bankId.toString());
        testCustomer.setConsentId("consent-123");
    }

    @Test
    void initiateKyc_ShouldCreateNewCustomer_WhenCustomerDoesNotExist() {
        // Arrange
        when(userFeignClient.getUserDetails(initiateRequest.getEmail())).thenReturn(testUserDto);
        when(bankCustomerRepository.findByUserIdAndBankId(
                        testUserDto.getUserId(), initiateRequest.getBankId().toString()))
                .thenReturn(Optional.empty());
        when(bankCustomerRepository.save(any(BankCustomer.class))).thenReturn(testCustomer);

        // Act
        BankCustomer result = kycOrchestrationService.initiateKyc(initiateRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testUserDto.getUserId(), result.getUserId());
        assertEquals(initiateRequest.getBankId().toString(), result.getBankId());
        assertNotNull(result.getConsentId());
        verify(consentRepository).save(argThat(consent -> consent.getStatus() == KycStatus.CONSENT_PENDING));
        verify(kafkaTemplate).send(eq("consent-request-topic"), any(ConsentRequestEvent.class));
        verify(kafkaLogProducer).logInfo(eq("KYC_INITIATED"), any());
    }

    @Test
    void initiateKyc_ShouldUpdateExistingCustomer_WhenCustomerExists() {
        // Arrange
        when(userFeignClient.getUserDetails(initiateRequest.getUserId())).thenReturn(testUserDto);
        when(bankCustomerRepository.findByUserIdAndBankId(
                        testUserDto.getUserId(), initiateRequest.getBankId().toString()))
                .thenReturn(Optional.of(testCustomer));
        when(bankCustomerRepository.save(any(BankCustomer.class))).thenReturn(testCustomer);

        // Act
        BankCustomer result = kycOrchestrationService.initiateKyc(initiateRequest);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getConsentId());
        verify(consentRepository).save(argThat(consent -> consent.getStatus() == KycStatus.CONSENT_PENDING));
        verify(kafkaTemplate).send(eq("consent-request-topic"), any(ConsentRequestEvent.class));
        verify(kafkaLogProducer).logInfo(eq("KYC_INITIATED"), any());
    }
}
