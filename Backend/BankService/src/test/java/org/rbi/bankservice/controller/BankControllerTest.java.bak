package org.kyc.management.bankservice.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
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
import org.kyc.management.bankservice.service.BankService;
import org.kyc.management.bankservice.service.KycOrchestrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(BankController.class)
class BankControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KycOrchestrationService orchestrator;

    @MockBean
    private BankService bankSvc;

    @Autowired
    private ObjectMapper objectMapper;

    private BankKycInitiateRequest initiateRequest;
    private BankKycSubmissionRequest submissionRequest;
    private BankCustomer testCustomer;
    private OtpRequestDto otpRequest;
    private OtpVerificationDto otpVerification;
    private BypassRequestDto bypassRequest;

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

        testCustomer = BankCustomer.builder()
                .id("customer-123")
                .userId("global-user-123")
                .bankId(bankId.toString())
                .kycStatus(KycStatus.VERIFIED)
                .build();

        otpRequest = OtpRequestDto.builder()
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        otpVerification = OtpVerificationDto.builder()
                .otp("123456")
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .role("CUSTOMER")
                .build();

        bypassRequest = BypassRequestDto.builder()
                .userId("user-123")
                .reason("Administrative override")
                .build();
    }

    @Test
    void initiate_ShouldReturnCustomer_WhenValidRequest() throws Exception {
        // Arrange
        when(orchestrator.initiateKyc(initiateRequest)).thenReturn(testCustomer);

        // Act & Assert
        mockMvc.perform(post("/bank/kyc/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initiateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(testCustomer.getUserId()))
                .andExpect(jsonPath("$.kycStatus").value(testCustomer.getKycStatus().toString()));
    }

    @Test
    void initiate_ShouldReturnBadRequest_WhenInvalidRequest() throws Exception {
        // Arrange
        BankKycInitiateRequest invalidRequest = new BankKycInitiateRequest(null, null, null, null);

        // Act & Assert
        mockMvc.perform(post("/bank/kyc/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submit_ShouldReturnKycResponse_WhenValidRequest() throws Exception {
        // Arrange
        KycResponseDto response = new KycResponseDto();
        response.setStatus("VERIFIED");
        response.setMessage("KYC verification successful");

        when(orchestrator.submitKycData(submissionRequest)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/bank/kyc/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submissionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"))
                .andExpect(jsonPath("$.message").value("KYC verification successful"));
    }

    @Test
    void getStatus_ShouldReturnCustomer_WhenCustomerExists() throws Exception {
        // Arrange
        String userId = "user-123";
        when(bankSvc.getCustomerStatus(userId)).thenReturn(testCustomer);

        // Act & Assert
        mockMvc.perform(get("/bank/kyc/status/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(testCustomer.getUserId()))
                .andExpect(jsonPath("$.kycStatus").value(testCustomer.getKycStatus().toString()));
    }

    @Test
    void adminBypass_ShouldReturnCustomer_WhenValidRequest() throws Exception {
        // Arrange
        when(bankSvc.bypassKyc(bypassRequest)).thenReturn(testCustomer);

        // Act & Assert
        mockMvc.perform(post("/bank/kyc/bypass")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bypassRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(testCustomer.getUserId()))
                .andExpect(jsonPath("$.kycStatus").value(testCustomer.getKycStatus().toString()));
    }

    @Test
    void requestOTP_ShouldReturnCreated_WhenOtpSentSuccessfully() throws Exception {
        // Arrange
        when(bankSvc.sendOtp(otpRequest)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/bank/kyc/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otpRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().string("OTP Sent successfully ..."));
    }

    @Test
    void requestOTP_ShouldReturnBadRequest_WhenOtpSendFails() throws Exception {
        // Arrange
        when(bankSvc.sendOtp(otpRequest)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/bank/kyc/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otpRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("OTP Sent failed ..."));
    }

    @Test
    void otpVerification_ShouldReturnOk_WhenValidOtp() throws Exception {
        // Arrange
        LoginResponseDto loginResponse = new LoginResponseDto();
        loginResponse.setEmail("test@example.com");
        loginResponse.setFirstName("John");
        loginResponse.setLastName("Doe");
        loginResponse.setRole("CUSTOMER");
        loginResponse.setToken("jwt-token-123");

        when(bankSvc.getLoginResponseDto(otpVerification)).thenReturn(loginResponse);

        // Act & Assert
        mockMvc.perform(post("/bank/kyc/otp-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otpVerification)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(loginResponse.getEmail()))
                .andExpect(jsonPath("$.token").value(loginResponse.getToken()));
    }

    @Test
    void logout_ShouldReturnOk_WhenValidToken() throws Exception {
        // Arrange
        String token = "Bearer valid.jwt.token";
        when(bankSvc.logout("valid.jwt.token")).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/bank/kyc/logout")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(content().string("Logged out successfully"));
    }

    @Test
    void logout_ShouldReturnBadRequest_WhenInvalidHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/bank/kyc/logout"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid Authorization header"));
    }

    @Test
    void logout_ShouldReturnInternalServerError_WhenLogoutFails() throws Exception {
        // Arrange
        String token = "Bearer invalid.jwt.token";
        when(bankSvc.logout("invalid.jwt.token")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/bank/kyc/logout")
                        .header("Authorization", token))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Logout failed"));
    }

    @Test
    void listApprovedConsents_ShouldReturnCustomerList() throws Exception {
        // Arrange
        List<BankCustomer> customers = List.of(testCustomer);
        when(bankSvc.getApprovedConsents()).thenReturn(customers);

        // Act & Assert
        mockMvc.perform(get("/bank/kyc/consents/approved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(testCustomer.getUserId()));
    }

    @Test
    void fetchCustomerData_ShouldReturnDataMap_WhenValidRequest() throws Exception {
        // Arrange
        String userId = "user-123";
        Map<String, Object> data = Map.of("identity", Map.of("name", "John Doe"));
        when(bankSvc.fetchKycData(userId)).thenReturn(data);

        // Act & Assert
        mockMvc.perform(post("/bank/kyc/fetch/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identity.name").value("John Doe"));
    }
}