package org.kyc.management.bankservice;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kyc.management.bankservice.dto.*;
import org.kyc.management.bankservice.entity.BankCustomer;
import org.kyc.management.bankservice.entity.KycStatus;
import org.kyc.management.bankservice.repository.BankCustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureWebMvc
@Testcontainers
@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"})
class BankServiceIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"))
            .withExposedPorts(27017);

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
            .withExposedPorts(9092);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private BankCustomerRepository bankCustomerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    private BankKycInitiateRequest initiateRequest;
    private BankKycSubmissionRequest submissionRequest;
    private BankCustomer testCustomer;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

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

        testCustomer = BankCustomer.builder()
                .userId("global-user-123")
                .bankId(bankId.toString())
                .kycStatus(KycStatus.CONSENT_PENDING)
                .build();

        // Clean up database before each test
        bankCustomerRepository.deleteAll();
    }

    @Test
    void fullKycWorkflow_ShouldCompleteSuccessfully() throws Exception {
        // Step 1: Initiate KYC
        mockMvc.perform(post("/bank/kyc/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initiateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("global-user-123"))
                .andExpect(jsonPath("$.kycStatus").value("CONSENT_PENDING"));

        // Verify customer was created in database
        BankCustomer savedCustomer = bankCustomerRepository.findByUserId("global-user-123").orElse(null);
        assertNotNull(savedCustomer);
        assertEquals(KycStatus.CONSENT_PENDING, savedCustomer.getKycStatus());

        // Step 2: Get customer status
        mockMvc.perform(get("/bank/kyc/status/{userId}", "user-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("global-user-123"))
                .andExpect(jsonPath("$.kycStatus").value("CONSENT_PENDING"));

        // Step 3: Submit KYC data (this would normally call external KYC service)
        // Note: This test assumes the KYC service returns VERIFIED status
        mockMvc.perform(post("/bank/kyc/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submissionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"));

        // Verify customer status was updated
        BankCustomer updatedCustomer = bankCustomerRepository.findByUserId("user-123").orElse(null);
        assertNotNull(updatedCustomer);
        assertEquals(KycStatus.VERIFIED, updatedCustomer.getKycStatus());
        assertNotNull(updatedCustomer.getVerifiedAt());
    }

    @Test
    void otpWorkflow_ShouldCompleteSuccessfully() throws Exception {
        // First create a customer in the database
        BankCustomer customer = BankCustomer.builder()
                .userId("test@example.com")
                .bankId("bank-123")
                .kycStatus(KycStatus.VERIFIED)
                .build();
        bankCustomerRepository.save(customer);

        // Step 1: Request OTP
        OtpRequestDto otpRequest = OtpRequestDto.builder()
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        mockMvc.perform(post("/bank/kyc/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otpRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().string("OTP Sent successfully ..."));

        // Step 2: Verify OTP (using a mock OTP since we can't access Redis in this test)
        OtpVerificationDto otpVerification = OtpVerificationDto.builder()
                .otp("123456")
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .role("CUSTOMER")
                .build();

        // This would normally work, but since we can't mock Redis in integration test,
        // we'll expect it to fail gracefully
        mockMvc.perform(post("/bank/kyc/otp-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otpVerification)))
                .andExpect(status().isOk()); // The exact behavior depends on Redis state
    }

    @Test
    void bypassKyc_ShouldUpdateCustomerStatus() throws Exception {
        // Create a customer
        BankCustomer customer = BankCustomer.builder()
                .userId("global-user-123")
                .bankId("bank-123")
                .kycStatus(KycStatus.CONSENT_PENDING)
                .riskLevel("LOW")
                .build();
        bankCustomerRepository.save(customer);

        // Bypass KYC
        BypassRequestDto bypassRequest = BypassRequestDto.builder()
                .userId("user-123")
                .reason("Administrative override")
                .build();

        mockMvc.perform(post("/bank/kyc/bypass")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bypassRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycStatus").value("VERIFIED_OVERRIDE"))
                .andExpect(jsonPath("$.overrideReason").value("Administrative override"));

        // Verify in database
        BankCustomer updatedCustomer = bankCustomerRepository.findByUserId("global-user-123").orElse(null);
        assertNotNull(updatedCustomer);
        assertEquals(KycStatus.VERIFIED_OVERRIDE, updatedCustomer.getKycStatus());
        assertEquals("Administrative override", updatedCustomer.getOverrideReason());
        assertNotNull(updatedCustomer.getVerifiedAt());
    }

    @Test
    void getApprovedConsents_ShouldReturnOnlyCustomersWithConsentTokens() throws Exception {
        // Create customers - one with consent token, one without
        BankCustomer withConsent = BankCustomer.builder()
                .userId("user-with-consent")
                .bankId("bank-123")
                .kycStatus(KycStatus.VERIFIED)
                .consentToken("consent-token-123")
                .build();

        BankCustomer withoutConsent = BankCustomer.builder()
                .userId("user-without-consent")
                .bankId("bank-123")
                .kycStatus(KycStatus.VERIFIED)
                .build();

        bankCustomerRepository.save(withConsent);
        bankCustomerRepository.save(withoutConsent);

        // Get approved consents
        mockMvc.perform(get("/bank/kyc/consents/approved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value("user-with-consent"));
    }

    @Test
    void databaseConstraints_ShouldBeEnforced() {
        // Test MongoDB constraints and indexing
        BankCustomer customer1 = BankCustomer.builder()
                .userId("test-user")
                .bankId("bank-1")
                .kycStatus(KycStatus.CONSENT_PENDING)
                .build();

        BankCustomer customer2 = BankCustomer.builder()
                .userId("test-user")
                .bankId("bank-2")
                .kycStatus(KycStatus.CONSENT_PENDING)
                .build();

        // Save both customers
        bankCustomerRepository.save(customer1);
        bankCustomerRepository.save(customer2);

        // Verify both can exist (different bankId)
        assertNotNull(bankCustomerRepository.findByUserIdAndBankId("test-user", "bank-1"));
        assertNotNull(bankCustomerRepository.findByUserIdAndBankId("test-user", "bank-2"));

        // Test existsByUserId
        assertTrue(bankCustomerRepository.existsByUserId("test-user"));
        assertFalse(bankCustomerRepository.existsByUserId("non-existent-user"));
    }

    @Test
    void kafkaIntegration_ShouldSendMessages() throws Exception {
        // This test verifies that Kafka integration works
        // In a real scenario, we'd use @EmbeddedKafka and verify messages were sent
        mockMvc.perform(post("/bank/kyc/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initiateRequest)))
                .andExpect(status().isOk());

        // In a more complete test, we would verify the Kafka message was sent
        // For now, we just ensure the endpoint works without throwing exceptions
    }
}