package org.rbi.kycmanagementservice;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rbi.kycmanagementservice.consumer.KycKafkaConsumer;
import org.rbi.kycmanagementservice.dto.UserProfileResponseDto;
import org.rbi.kycmanagementservice.entity.Address;
import org.rbi.kycmanagementservice.entity.KycEntity;
import org.rbi.kycmanagementservice.entity.KycRecord;
import org.rbi.kycmanagementservice.event.DigiLockerDataFetchedEvent;
import org.rbi.kycmanagementservice.logging.KafkaLogProducer;
import org.rbi.kycmanagementservice.repository.KycRecordRepository;
import org.rbi.kycmanagementservice.repository.KycRepository;
import org.rbi.kycmanagementservice.service.JwtParserUtil;
import org.rbi.kycmanagementservice.service.KycService;
import org.rbi.kycmanagementservice.service.KycTokenRedisService;
import org.rbi.kycmanagementservice.service.TokenBlacklistService;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KycManagementServiceTests {

    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private JwtParserUtil jwtParserUtil;

    @Mock private KycRepository kycRepository;
    @Mock private KycRecordRepository kycRecordRepository;
    @Mock private KycTokenRedisService kycTokenRedisService;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private KafkaLogProducer kafkaLogProducer;

    private KycService kycService;
    private KycKafkaConsumer kycKafkaConsumer;

    @BeforeEach
    void setUp() {
        jwtParserUtil = new JwtParserUtil(TEST_SECRET);
        kycService = new KycService(kycRepository, kycRecordRepository, kycTokenRedisService, jwtParserUtil, tokenBlacklistService);
        kycKafkaConsumer = new KycKafkaConsumer(kycRecordRepository, kycRepository, kafkaTemplate, kafkaLogProducer);
    }

    private String createValidToken(String userId, List<String> fields, long ttlMillis) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId)
                .claim("userId", userId)
                .claim("documentFields", fields)
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMillis))
                .signWith(key)
                .compact();
    }

    @Test
    @DisplayName("Cryptographic JWT signature verification succeeds for valid token")
    void testValidJwtVerification() {
        String token = createValidToken("user-123", List.of("aadhaarNo", "pan"), 3600000);
        String userId = jwtParserUtil.extractUserId(token);
        assertEquals("user-123", userId);

        List<String> fields = jwtParserUtil.extractRequestedFields(token);
        assertTrue(fields.contains("aadhaarNo"));
        assertTrue(fields.contains("pan"));
        assertFalse(jwtParserUtil.isTokenExpired(token));
    }

    @Test
    @DisplayName("Forged token with incorrect secret is rejected by cryptographic parser")
    void testForgedTokenRejected() {
        SecretKey attackerKey = Keys.hmacShaKeyFor("AttackerSecretKeyMustBeLongEnough1234567890!".getBytes(StandardCharsets.UTF_8));
        String forgedToken = Jwts.builder()
                .subject("admin-user")
                .signWith(attackerKey)
                .compact();

        assertThrows(Exception.class, () -> jwtParserUtil.parsePayload(forgedToken));
    }

    @Test
    @DisplayName("Bank access retrieves filtered fields when user is VERIFIED")
    void testGetVerifiedDataForBank() {
        String token = createValidToken("user-999", List.of("aadhaarNo"), 3600000);

        KycEntity entity = KycEntity.builder()
                .userId("user-999")
                .aadhaarNo("1234-5678-9012")
                .pan("ABCDE1234F")
                .status("VERIFIED")
                .build();

        when(tokenBlacklistService.isBlacklisted(any())).thenReturn(false);
        when(kycRepository.findById("user-999")).thenReturn(Optional.of(entity));

        Map<String, Object> data = kycService.getVerifiedDataForBank(token);
        assertNotNull(data);
        assertEquals("1234-5678-9012", data.get("aadhaarNo"));
        assertNull(data.get("pan")); // Filtered out because only aadhaarNo was in token
    }

    @Test
    @DisplayName("KycKafkaConsumer handles Digilocker event and updates KycEntity and KycRecord")
    void testKafkaConsumerDigilockerData() {
        DigiLockerDataFetchedEvent event = DigiLockerDataFetchedEvent.builder()
                .userId("user-555")
                .mobileNumber("9876543210")
                .email("test@rbi.org")
                .aadharNumber("1111-2222-3333")
                .panNumber("ABCDE9999Z")
                .firstName("John")
                .lastName("Doe")
                .dob(LocalDate.of(1990, 1, 1))
                .build();

        when(kycRecordRepository.findByUserId("user-555")).thenReturn(Optional.empty());
        when(kycRecordRepository.findByMobileNumber("9876543210")).thenReturn(Optional.empty());
        when(kycRepository.findById("user-555")).thenReturn(Optional.empty());

        kycKafkaConsumer.consumeDigilockerData(event);

        verify(kycRecordRepository, times(1)).save(argThat(r ->
                "VERIFIED".equals(r.getStatus()) && "9876543210".equals(r.getMobileNumber())
        ));
        verify(kycRepository, times(1)).save(argThat(e ->
                "VERIFIED".equals(e.getStatus()) && "user-555".equals(e.getUserId())
        ));
    }
}
