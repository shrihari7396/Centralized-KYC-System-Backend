package org.rbi.consentservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.rbi.consentservice.client.BankFeignClient;
import org.rbi.consentservice.client.UserFeignClient;
import org.rbi.consentservice.entity.ConsentRecord;
import org.rbi.consentservice.enums.ConsentStatus;
import org.rbi.consentservice.exception.ConsentNotFoundException;
import org.rbi.consentservice.exception.InValidOtpException;
import org.rbi.consentservice.kafka.ConsentKafkaProducer;
import org.rbi.consentservice.logging.KafkaLogProducer;
import org.rbi.consentservice.repository.ConsentRepository;
import org.rbi.consentservice.service.ConsentServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsentServiceTests {

    private ConsentRepository consentRepository;
    private UserFeignClient userFeignClient;
    private BankFeignClient bankFeignClient;
    private ConsentKafkaProducer consentKafkaProducer;
    private KafkaLogProducer kafkaLogProducer;
    private ObjectMapper objectMapper;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private KafkaTemplate<String, Object> kafkaTemplate;
    private ConsentServiceImpl consentService;

    @BeforeEach
    void setUp() {
        consentRepository = Mockito.mock(ConsentRepository.class);
        userFeignClient = Mockito.mock(UserFeignClient.class);
        bankFeignClient = Mockito.mock(BankFeignClient.class);
        consentKafkaProducer = Mockito.mock(ConsentKafkaProducer.class);
        kafkaLogProducer = Mockito.mock(KafkaLogProducer.class);
        objectMapper = new ObjectMapper();
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOperations = Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        kafkaTemplate = Mockito.mock(KafkaTemplate.class);

        consentService = new ConsentServiceImpl(
                consentRepository,
                userFeignClient,
                bankFeignClient,
                consentKafkaProducer,
                kafkaLogProducer,
                objectMapper,
                redisTemplate,
                kafkaTemplate,
                new SecureRandom()
        );
    }

    @Test
    void whenConsentNotFound_thenThrowsException() {
        UUID consentId = UUID.randomUUID();
        when(consentRepository.findById(consentId.toString())).thenReturn(Optional.empty());

        assertThrows(ConsentNotFoundException.class, () -> consentService.sendConsentOtp(consentId));
    }

    @Test
    void whenConsentIsPending_thenGeneratesOtpWithoutLoggingPlaintext() {
        UUID consentId = UUID.randomUUID();
        ConsentRecord record = ConsentRecord.builder()
                .id(consentId.toString())
                .userId("user-123")
                .bankId("bank-456")
                .consentStatus(ConsentStatus.PENDING)
                .build();

        when(consentRepository.findById(consentId.toString())).thenReturn(Optional.of(record));
        when(userFeignClient.getUserDetailsById("user-123")).thenReturn(
                org.rbi.consentservice.dto.UserDto.builder()
                        .userId("user-123")
                        .email("user@example.com")
                        .firstName("Test")
                        .lastName("User")
                        .build()
        );

        consentService.sendConsentOtp(consentId);

        verify(valueOperations).set(eq("consent_otp:" + consentId.toString()), any(), eq(3L), any());
    }
}
