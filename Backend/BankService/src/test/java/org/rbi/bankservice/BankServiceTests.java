package org.rbi.bankservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.rbi.bankservice.client.UserFeignClient;
import org.rbi.bankservice.entity.BankCustomer;
import org.rbi.bankservice.exception.InValidOtpException;
import org.rbi.bankservice.logging.KafkaLogProducer;
import org.rbi.bankservice.repository.BankCustomerMongoRepository;
import org.rbi.bankservice.repository.BankCustomerRepository;
import org.rbi.bankservice.repository.BankRepository;
import org.rbi.bankservice.repository.ConsentRepository;
import org.rbi.bankservice.security.JwtValidator;
import org.rbi.bankservice.service.BankServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BankServiceTests {

    private BankCustomerRepository repo;
    private KafkaLogProducer kafkaLogProducer;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private KafkaTemplate<String, Object> kafkaTemplate;
    private JwtValidator jwtValidator;
    private BankCustomerMongoRepository mongoRepo;
    private UserFeignClient userFeignClient;
    private BankRepository bankRepo;
    private ConsentRepository consentRepo;
    private BankServiceImpl bankService;

    @BeforeEach
    void setUp() {
        repo = Mockito.mock(BankCustomerRepository.class);
        kafkaLogProducer = Mockito.mock(KafkaLogProducer.class);
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOperations = Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        jwtValidator = Mockito.mock(JwtValidator.class);
        mongoRepo = Mockito.mock(BankCustomerMongoRepository.class);
        userFeignClient = Mockito.mock(UserFeignClient.class);
        bankRepo = Mockito.mock(BankRepository.class);
        consentRepo = Mockito.mock(ConsentRepository.class);

        bankService = new BankServiceImpl(
                repo,
                kafkaLogProducer,
                redisTemplate,
                kafkaTemplate,
                new SecureRandom(),
                jwtValidator,
                mongoRepo,
                userFeignClient,
                bankRepo,
                consentRepo
        );
    }

    @Test
    void whenOtpFails3Times_thenLocksAccount() {
        String key = "bankuser@example.com";
        when(redisTemplate.hasKey("otp_locked:" + key)).thenReturn(false);
        when(valueOperations.get(key)).thenReturn("654321");
        when(valueOperations.increment("otp_attempts:" + key)).thenReturn(3L);

        assertThrows(InValidOtpException.class, () -> bankService.verifyOtp("000000", key));
        verify(valueOperations).set(eq("otp_locked:" + key), eq("locked"), any());
    }

    @Test
    void whenOtpCorrect_thenSucceedsAndClearsAttempts() {
        String key = "bankuser@example.com";
        when(redisTemplate.hasKey("otp_locked:" + key)).thenReturn(false);
        when(valueOperations.get(key)).thenReturn("654321");

        boolean result = bankService.verifyOtp("654321", key);

        assertTrue(result);
        verify(redisTemplate).delete("otp_attempts:" + key);
        verify(redisTemplate).delete(key);
    }
}
