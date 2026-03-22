package org.kyc.management.bankservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kyc.management.bankservice.client.KycFeignClient;
import org.kyc.management.bankservice.client.UserFeignClient;
import org.kyc.management.bankservice.dto.*;
import org.kyc.management.bankservice.entity.BankCustomer;
import org.kyc.management.bankservice.entity.KycStatus;
import org.kyc.management.bankservice.event.CreateOtpEvent;
import org.kyc.management.bankservice.exception.InValidOtpException;
import org.kyc.management.bankservice.logging.KafkaLogProducer;
import org.kyc.management.bankservice.repository.BankCustomerMongoRepository;
import org.kyc.management.bankservice.repository.BankCustomerRepository;
import org.kyc.management.bankservice.repository.BankRepository;
import org.kyc.management.bankservice.repository.ConsentRepository;
import org.kyc.management.bankservice.entity.Consent;
import org.kyc.management.bankservice.security.JwtValidator;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class BankServiceImplTest {

    @Mock private BankCustomerRepository repo;

    @Mock private KafkaLogProducer kafkaLogProducer;

    @Mock private RedisTemplate<String, String> redisTemplate;

    @Mock private ValueOperations<String, String> valueOperations;

    @Mock private KafkaTemplate<String, CreateOtpEvent> kafkaTemplate;

    @Mock private SecureRandom secureRandom;

    @Mock private JwtValidator jwtValidator;

    @Mock private KycFeignClient kycClient;

    @Mock private BankCustomerMongoRepository mongoRepo;

    @Mock private UserFeignClient userFeignClient;
    @Mock private BankRepository bankRepo;
    @Mock private ConsentRepository consentRepo;

    @InjectMocks private BankServiceImpl bankService;

    private BankCustomer testCustomer;
    private UserDto testUserDto;

    @BeforeEach
    void setUp() {
        testUserDto =
                new UserDto(
                        "global-user-123",
                        "John",
                        "Doe",
                        "test@example.com",
                        "1234567890",
                        Arrays.asList("CUSTOMER"),
                        "ACTIVE");

        testCustomer = new BankCustomer();
        testCustomer.setId("customer-123");
        testCustomer.setUserId("global-user-123");
        testCustomer.setBankId("bank-456");
        testCustomer.setConsentId("consent-123");
        testCustomer.setRiskLevel("LOW");
        testCustomer.setCreatedAt(LocalDateTime.now());

        // Setup Redis mock
        // Removed global Redis mock to avoid unnecessary stubbing
    }

    @Test
    void getCustomerStatus_ShouldReturnCustomer_WhenCustomerExists() {
        // Arrange
        String userId = "user-123";
        when(userFeignClient.getUserDetails(userId)).thenReturn(testUserDto);
        when(repo.findByUserId(testUserDto.getUserId())).thenReturn(Optional.of(testCustomer));

        // Act
        BankCustomer result = bankService.getCustomerStatus(userId);

        // Assert
        assertNotNull(result);
        assertEquals(testCustomer.getUserId(), result.getUserId());
        verify(kafkaLogProducer).logInfo(eq("GET_CUSTOMER_STATUS"), any());
    }

    @Test
    void getCustomerStatus_ShouldThrowException_WhenCustomerNotFound() {
        // Arrange
        String userId = "non-existent-user";
        when(userFeignClient.getUserDetails(userId)).thenReturn(testUserDto);
        when(repo.findByUserId(testUserDto.getUserId())).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> bankService.getCustomerStatus(userId));
        assertEquals("Customer not found", exception.getMessage());
        verify(kafkaLogProducer).logError(eq("CUSTOMER_NOT_FOUND"), any());
    }

    @Test
    void bypassKyc_ShouldUpdateStatus_WhenValidRequest() {
        // Arrange
        BypassRequestDto request =
                BypassRequestDto.builder()
                        .userId("user-123")
                        .reason("Administrative override")
                        .build();

        when(userFeignClient.getUserDetails(request.getUserId())).thenReturn(testUserDto);
        when(repo.findByUserId(testUserDto.getUserId())).thenReturn(Optional.of(testCustomer));
        when(repo.save(any(BankCustomer.class))).thenReturn(testCustomer);

        // Act
        BankCustomer result = bankService.bypassKyc(request);

        // Assert
        assertEquals(KycStatus.VERIFIED_OVERRIDE, result.getKycStatus());
        assertEquals(request.getReason(), result.getOverrideReason());
        assertNotNull(result.getVerifiedAt());
        verify(kafkaLogProducer).logInfo(eq("BYPASS_KYC_SUCCESS"), any());
    }

    @Test
    void bypassKyc_ShouldThrowException_WhenHighRiskUser() {
        // Arrange
        testCustomer.setRiskLevel("HIGH_RISK");
        BypassRequestDto request =
                BypassRequestDto.builder()
                        .userId("user-123")
                        .reason("Administrative override")
                        .build();

        when(userFeignClient.getUserDetails(request.getUserId())).thenReturn(testUserDto);
        when(repo.findByUserId(testUserDto.getUserId())).thenReturn(Optional.of(testCustomer));

        // Act & Assert
        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> bankService.bypassKyc(request));
        assertEquals("Cannot bypass KYC for HIGH_RISK user", exception.getMessage());
        verify(kafkaLogProducer).logWarning(eq("BYPASS_KYC_REJECTED"), any());
    }

    @Test
    void sendOtp_ShouldSendOtp_WhenValidRequest() {
        // Arrange
        OtpRequestDto request =
                OtpRequestDto.builder()
                        .email("test@example.com")
                        .firstName("John")
                        .lastName("Doe")
                        .build();

        when(userFeignClient.getUserDetails(request.getEmail())).thenReturn(testUserDto);
        when(repo.existsByUserId(testUserDto.getUserId())).thenReturn(true);
        when(secureRandom.nextInt(1_000_000)).thenReturn(123456);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        Boolean result = bankService.sendOtp(request);

        // Assert
        assertTrue(result);
        verify(kafkaTemplate).send(eq("OTP_CREATED_TOPIC"), any(CreateOtpEvent.class));
        verify(valueOperations).set(eq(request.getEmail()), eq("123456"), any(Duration.class));
        verify(kafkaLogProducer).logInfo(eq("OTP_SENT"), any());
    }

    @Test
    void sendOtp_ShouldThrowException_WhenEmailNotRegistered() {
        // Arrange
        OtpRequestDto request =
                OtpRequestDto.builder()
                        .email("nonexistent@example.com")
                        .firstName("John")
                        .lastName("Doe")
                        .build();

        // Update testUserDto to match the email for consistency in stubbing if needed, 
        // or just use a generic UserDto. We'll use a local one for clarity if needed, 
        // but testUserDto with a mock response is fine.
        UserDto anonymousUser = UserDto.builder().userId("anon-123").email(request.getEmail()).build();
        when(userFeignClient.getUserDetails(request.getEmail())).thenReturn(anonymousUser);
        when(repo.existsByUserId(anonymousUser.getUserId())).thenReturn(false);

        // Act & Assert
        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> bankService.sendOtp(request));
        assertEquals("Email address is not registered with the Bank", exception.getMessage());
        verify(kafkaLogProducer).logError(eq("LOGIN_REJECTED"), any());
    }

    @Test
    void verifyOtp_ShouldReturnTrue_WhenOtpValid() {
        // Arrange
        String otp = "123456";
        String key = "test@example.com";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(otp);

        // Act
        Boolean result = bankService.verifyOtp(otp, key);

        // Assert
        assertTrue(result);
        verify(redisTemplate).delete(key);
        verify(kafkaLogProducer).logInfo(eq("OTP_VERIFIED"), any());
    }

    @Test
    void verifyOtp_ShouldThrowException_WhenOtpExpired() {
        // Arrange
        String otp = "123456";
        String key = "test@example.com";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(null);

        // Act & Assert
        InValidOtpException exception =
                assertThrows(InValidOtpException.class, () -> bankService.verifyOtp(otp, key));
        assertEquals("Expired or invalid OTP", exception.getMessage());
        verify(kafkaLogProducer).logWarning(eq("OTP_VERIFICATION_FAILED_EXPIRED"), any());
    }

    @Test
    void verifyOtp_ShouldThrowException_WhenOtpInvalid() {
        // Arrange
        String otp = "123456";
        String key = "test@example.com";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn("654321");

        // Act & Assert
        InValidOtpException exception =
                assertThrows(InValidOtpException.class, () -> bankService.verifyOtp(otp, key));
        assertEquals("Invalid OTP", exception.getMessage());
        verify(kafkaLogProducer).logWarning(eq("OTP_VERIFICATION_FAILED_INVALID"), any());
    }

    @Test
    void getLoginResponseDto_ShouldReturnResponse_WhenOtpValid() {
        // Arrange
        OtpVerificationDto request =
                OtpVerificationDto.builder()
                        .otp("123456")
                        .email("test@example.com")
                        .firstName("John")
                        .lastName("Doe")
                        .role("CUSTOMER")
                        .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(request.getEmail())).thenReturn(request.getOtp());
        when(jwtValidator.generateToken(request.getEmail(), request.getRole()))
                .thenReturn("jwt-token-123");

        // Act
        LoginResponseDto result = bankService.getLoginResponseDto(request);

        // Assert
        assertNotNull(result);
        assertEquals(request.getEmail(), result.getEmail());
        assertEquals(request.getRole(), result.getRole());
        assertEquals("jwt-token-123", result.getToken());
        verify(kafkaLogProducer).logInfo(eq("USER_LOGGED_IN"), any());
    }

    @Test
    void getLoginResponseDto_ShouldThrowException_WhenOtpInvalid() {
        // Arrange
        OtpVerificationDto request =
                OtpVerificationDto.builder()
                        .otp("123456")
                        .email("test@example.com")
                        .firstName("John")
                        .lastName("Doe")
                        .role("CUSTOMER")
                        .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(request.getEmail())).thenReturn("654321");

        // Act & Assert
        InValidOtpException exception =
                assertThrows(
                        InValidOtpException.class, () -> bankService.getLoginResponseDto(request));
        assertEquals("Invalid OTP", exception.getMessage());
        verify(kafkaLogProducer).logWarning(eq("OTP_VERIFICATION_FAILED_INVALID"), any());
    }

    @Test
    void logout_ShouldReturnTrue_WhenValidToken() {
        // Arrange
        String token = "valid.jwt.token";
        String jti = "jti-123";

        when(jwtValidator.getJtiFromToken(token)).thenReturn(jti);
        when(jwtValidator.getExpirationFromToken(token))
                .thenReturn(new java.util.Date(System.currentTimeMillis() + 3600000));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        Boolean result = bankService.logout(token);

        // Assert
        assertTrue(result);
        verify(valueOperations).set(eq("blacklist:" + jti), eq("true"), any(Duration.class));
    }

    @Test
    void logout_ShouldReturnFalse_WhenTokenExpired() {
        // Arrange
        String token = "expired.jwt.token";
        String jti = "jti-123";

        when(jwtValidator.getJtiFromToken(token)).thenReturn(jti);
        when(jwtValidator.getExpirationFromToken(token))
                .thenReturn(new java.util.Date(System.currentTimeMillis() - 1000));

        // Act
        Boolean result = bankService.logout(token);

        // Assert
        assertFalse(result);
    }

    @Test
    void getApprovedConsents_ShouldReturnCustomersWithConsentTokens() {
        // Arrange
        BankCustomer customerWithConsent = testCustomer;
        BankCustomer customerWithoutConsent =
                BankCustomer.builder()
                        .id("customer-456")
                        .userId("global-user-456")
                        .bankId("bank-456")
                        .kycStatus(KycStatus.CONSENT_PENDING)
                        .build();

        when(repo.findAll()).thenReturn(List.of(customerWithConsent, customerWithoutConsent));

        // Act
        List<BankCustomer> result = bankService.getApprovedConsents();

        // Assert
        assertEquals(1, result.size());
        assertEquals(testCustomer.getId(), result.get(0).getId());
        verify(kafkaLogProducer).logInfo(eq("GET_APPROVED_CONSENTS"), any());
    }

    @Test
    void fetchKycData_ShouldReturnData_WhenConsentAvailable() {
        // Arrange
        String userId = "user-123";
        Map<String, Object> kycData =
                Map.of("identity", Map.of("name", "John Doe"), "documents", List.of("passport"));

        when(userFeignClient.getUserDetails(userId)).thenReturn(testUserDto);
        when(repo.findByUserId(testUserDto.getUserId())).thenReturn(Optional.of(testCustomer));
        when(kycClient.fetchKycData("Bearer " + testCustomer.getConsentToken()))
                .thenReturn(kycData);
        when(mongoRepo.save(any())).thenReturn(null);

        // Act
        Map<String, Object> result = bankService.fetchKycData(userId);

        // Assert
        assertEquals(kycData, result);
        verify(mongoRepo).save(any());
        verify(repo).save(any(BankCustomer.class));
        verify(kafkaLogProducer).logInfo(eq("SAVED_KYC_MONGO"), any());
    }

    @Test
    void fetchKycData_ShouldThrowException_WhenNoConsentToken() {
        // Arrange
        String userId = "user-123";
        testCustomer.setConsentToken(null);

        when(userFeignClient.getUserDetails(userId)).thenReturn(testUserDto);
        when(repo.findByUserId(testUserDto.getUserId())).thenReturn(Optional.of(testCustomer));

        // Act & Assert
        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> bankService.fetchKycData(userId));
        assertEquals("No approved consent token available for this user.", exception.getMessage());
        verify(kafkaLogProducer).logError(eq("FETCH_KYC_FAILED"), any());
    }
}
