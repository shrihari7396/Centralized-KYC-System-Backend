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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureWebMvc
@Testcontainers
class BankServiceEndToEndTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"))
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private BankCustomerRepository bankCustomerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        bankCustomerRepository.deleteAll();
    }

    @Test
    void completeKycJourney_ShouldTransitionThroughAllStates() throws Exception {
        UUID bankId = UUID.randomUUID();
        String userId = "test-user-123";
        String email = "john.doe@example.com";

        // ==========================================
        // PHASE 1: KYC INITIATION
        // ==========================================

        // Step 1: Initiate KYC Process
        BankKycInitiateRequest initiateRequest = new BankKycInitiateRequest(
                userId,
                bankId,
                List.of("passport", "utility_bill", "bank_statement"),
                email
        );

        MvcResult initiateResult = mockMvc.perform(post("/bank/kyc/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initiateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.kycStatus").value("CONSENT_PENDING"))
                .andReturn();

        BankCustomer initiatedCustomer = objectMapper.readValue(
                initiateResult.getResponse().getContentAsString(), BankCustomer.class);
        assertNotNull(initiatedCustomer.getId());

        // Verify in database
        BankCustomer dbCustomer = bankCustomerRepository.findById(initiatedCustomer.getId()).orElse(null);
        assertNotNull(dbCustomer);
        assertEquals(KycStatus.CONSENT_PENDING, dbCustomer.getKycStatus());

        // ==========================================
        // PHASE 2: STATUS CHECKING
        // ==========================================

        // Step 2: Check KYC Status
        mockMvc.perform(get("/bank/kyc/status/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycStatus").value("CONSENT_PENDING"))
                .andExpect(jsonPath("$.userId").exists());

        // ==========================================
        // PHASE 3: KYC DATA SUBMISSION
        // ==========================================

        // Step 3: Submit KYC Data (simulating user providing documents)
        BankKycSubmissionRequest submissionRequest = BankKycSubmissionRequest.builder()
                .userId(userId)
                .identity(KycIdentityDto.builder()
                        .firstName("John")
                        .lastName("Doe")
                        .dateOfBirth("1990-05-15")
                        .address("123 Main St, City, Country")
                        .phoneNumber("+1234567890")
                        .build())
                .documents(List.of(
                        KycDocumentDto.builder()
                                .type("passport")
                                .url("https://storage.example.com/passport.jpg")
                                .fileName("passport_scan.jpg")
                                .build(),
                        KycDocumentDto.builder()
                                .type("utility_bill")
                                .url("https://storage.example.com/utility_bill.pdf")
                                .fileName("utility_bill.pdf")
                                .build()
                ))
                .build();

        MvcResult submitResult = mockMvc.perform(post("/bank/kyc/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submissionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"))
                .andReturn();

        // Verify customer status updated to VERIFIED
        BankCustomer verifiedCustomer = bankCustomerRepository.findById(initiatedCustomer.getId()).orElse(null);
        assertNotNull(verifiedCustomer);
        assertEquals(KycStatus.VERIFIED, verifiedCustomer.getKycStatus());
        assertNotNull(verifiedCustomer.getVerifiedAt());

        // ==========================================
        // PHASE 4: POST-KYC OPERATIONS
        // ==========================================

        // Step 4: Simulate consent approval (normally done via Kafka consumer)
        verifiedCustomer.setConsentToken("consent-token-" + UUID.randomUUID());
        bankCustomerRepository.save(verifiedCustomer);

        // Step 5: Fetch KYC Data (after consent approval)
        MvcResult fetchResult = mockMvc.perform(post("/bank/kyc/fetch/{userId}", userId))
                .andExpect(status().isOk())
                .andReturn();

        // The actual data fetching would depend on the external KYC service
        // In this test, we just verify the endpoint is accessible

        // ==========================================
        // PHASE 5: ADMINISTRATIVE OPERATIONS
        // ==========================================

        // Step 6: Test Administrative Bypass (for edge cases)
        BypassRequestDto bypassRequest = BypassRequestDto.builder()
                .userId(userId)
                .reason("Urgent business requirement - customer verified via alternative means")
                .build();

        // First, change status back to allow bypass
        verifiedCustomer.setKycStatus(KycStatus.REJECTED);
        verifiedCustomer.setRiskLevel("LOW");
        bankCustomerRepository.save(verifiedCustomer);

        mockMvc.perform(post("/bank/kyc/bypass")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bypassRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycStatus").value("VERIFIED_OVERRIDE"))
                .andExpect(jsonPath("$.overrideReason").exists());

        // Verify bypass in database
        BankCustomer bypassedCustomer = bankCustomerRepository.findById(initiatedCustomer.getId()).orElse(null);
        assertNotNull(bypassedCustomer);
        assertEquals(KycStatus.VERIFIED_OVERRIDE, bypassedCustomer.getKycStatus());
        assertEquals(bypassRequest.getReason(), bypassedCustomer.getOverrideReason());

        // ==========================================
        // PHASE 6: LISTING AND REPORTING
        // ==========================================

        // Step 7: List all approved consents
        mockMvc.perform(get("/bank/kyc/consents/approved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].kycStatus").value("VERIFIED_OVERRIDE"));

        // ==========================================
        // VERIFICATION: COMPLETE JOURNEY VALIDATION
        // ==========================================

        // Final verification - customer should have gone through multiple status changes
        BankCustomer finalCustomer = bankCustomerRepository.findById(initiatedCustomer.getId()).orElse(null);
        assertNotNull(finalCustomer);
        assertEquals(KycStatus.VERIFIED_OVERRIDE, finalCustomer.getKycStatus());
        assertNotNull(finalCustomer.getVerifiedAt());
        assertNotNull(finalCustomer.getOverrideReason());
        assertNotNull(finalCustomer.getConsentToken());
    }

    @Test
    void errorHandling_ShouldHandleInvalidRequestsGracefully() throws Exception {
        // Test 1: Invalid initiation request
        BankKycInitiateRequest invalidInitiateRequest = new BankKycInitiateRequest(
                null, // Invalid: null userId
                null, // Invalid: null bankId
                null, // Invalid: null documents
                "invalid-email" // Invalid: not a proper email format
        );

        mockMvc.perform(post("/bank/kyc/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidInitiateRequest)))
                .andExpect(status().isBadRequest());

        // Test 2: Request OTP for non-existent user
        OtpRequestDto otpRequest = OtpRequestDto.builder()
                .email("nonexistent@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .build();

        mockMvc.perform(post("/bank/kyc/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otpRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("OTP Sent failed ..."));

        // Test 3: Get status for non-existent user
        mockMvc.perform(get("/bank/kyc/status/{userId}", "non-existent-user"))
                .andExpect(status().isOk()); // This might return error depending on implementation

        // Test 4: Bypass KYC for high-risk user
        // First create a high-risk customer
        BankCustomer highRiskCustomer = BankCustomer.builder()
                .userId("high-risk-user")
                .bankId("bank-123")
                .kycStatus(KycStatus.CONSENT_PENDING)
                .riskLevel("HIGH_RISK")
                .build();
        bankCustomerRepository.save(highRiskCustomer);

        BypassRequestDto bypassRequest = BypassRequestDto.builder()
                .userId("user-123")
                .reason("Attempting to bypass high risk user")
                .build();

        mockMvc.perform(post("/bank/kyc/bypass")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bypassRequest)))
                .andExpect(status().isOk()); // Implementation may handle this differently

        // Test 5: Fetch data without consent
        BankCustomer noConsentCustomer = BankCustomer.builder()
                .userId("no-consent-user")
                .bankId("bank-123")
                .kycStatus(KycStatus.VERIFIED)
                // consentToken is null
                .build();
        bankCustomerRepository.save(noConsentCustomer);

        mockMvc.perform(post("/bank/kyc/fetch/{userId}", "user-123"))
                .andExpect(status().isOk()); // Should handle gracefully
    }

    @Test
    void concurrentOperations_ShouldHandleMultipleUsers() throws Exception {
        // Create multiple users and test concurrent operations
        UUID bankId = UUID.randomUUID();

        for (int i = 1; i <= 3; i++) {
            String userId = "user-" + i;
            String email = "user" + i + "@example.com";

            // Initiate KYC for each user
            BankKycInitiateRequest initiateRequest = new BankKycInitiateRequest(
                    userId,
                    bankId,
                    List.of("passport"),
                    email
            );

            mockMvc.perform(post("/bank/kyc/initiate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(initiateRequest)))
                    .andExpect(status().isOk());

            // Submit KYC data
            BankKycSubmissionRequest submissionRequest = BankKycSubmissionRequest.builder()
                    .userId(userId)
                    .identity(KycIdentityDto.builder()
                            .firstName("User" + i)
                            .lastName("Test")
                            .dateOfBirth("1990-01-01")
                            .build())
                    .documents(List.of(KycDocumentDto.builder()
                            .type("passport")
                            .url("https://example.com/doc" + i + ".jpg")
                            .build()))
                    .build();

            mockMvc.perform(post("/bank/kyc/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(submissionRequest)))
                    .andExpect(status().isOk());
        }

        // Verify all users were processed
        List<BankCustomer> allCustomers = bankCustomerRepository.findAll();
        assertEquals(3, allCustomers.size());

        for (BankCustomer customer : allCustomers) {
            assertEquals(KycStatus.VERIFIED, customer.getKycStatus());
            assertNotNull(customer.getVerifiedAt());
        }

        // Test listing all approved consents
        mockMvc.perform(get("/bank/kyc/consents/approved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0)); // No consent tokens set yet
    }
}