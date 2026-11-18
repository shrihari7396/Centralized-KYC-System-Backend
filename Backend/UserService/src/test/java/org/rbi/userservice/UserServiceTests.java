package org.rbi.userservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.rbi.userservice.DTO.OtpRequestDto;
import org.rbi.userservice.DTO.OtpVerificationDto;
import org.rbi.userservice.DTO.RegisterDTO;
import org.rbi.userservice.Model.Role;
import org.rbi.userservice.Model.UserEntity;
import org.rbi.userservice.Model.UserStatus;
import org.rbi.userservice.Repository.UserRepository;
import org.rbi.userservice.exception.InValidOtpException;
import org.rbi.userservice.kafkaEvents.CreateOtpEvent;
import org.rbi.userservice.logging.KafkaLogProducer;
import org.rbi.userservice.service.JWTService;
import org.rbi.userservice.service.impl.UserServiceImpl;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;

import java.security.SecureRandom;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserServiceTests {

    private RedisTemplate<String, String> redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private KafkaTemplate<String, CreateOtpEvent> kafkaTemplate;
    private KafkaLogProducer kafkaLogProducer;
    private JWTService jwtService;
    private UserRepository userRepository;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        redisTemplate = Mockito.mock(RedisTemplate.class);
        valueOperations = Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        kafkaLogProducer = Mockito.mock(KafkaLogProducer.class);
        jwtService = Mockito.mock(JWTService.class);
        userRepository = Mockito.mock(UserRepository.class);

        userService = new UserServiceImpl(
                redisTemplate,
                kafkaTemplate,
                kafkaLogProducer,
                jwtService,
                userRepository,
                new SecureRandom()
        );
    }

    @Test
    void whenRegisteringNewUser_thenSavesWithUserRoleAndKycPending() {
        RegisterDTO registerDTO = RegisterDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .mobileNumber("+919876543210")
                .build();

        when(userRepository.findByEmail(registerDTO.getEmail())).thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean registered = userService.registerUser(registerDTO);

        assertTrue(registered);
        verify(userRepository, times(1)).save(argThat(entity ->
                entity.getRoles() == Role.USER &&
                entity.getStatus() == UserStatus.KYC_PENDING &&
                entity.getEmail().equals("john.doe@example.com")
        ));
    }

    @Test
    void whenOtpFails3Times_thenLocksAccountFor15Minutes() {
        String email = "test@example.com";
        String wrongOtp = "000000";

        when(redisTemplate.hasKey("otp_locked:" + email)).thenReturn(false);
        when(valueOperations.get(email)).thenReturn("123456");
        when(valueOperations.increment("otp_attempts:" + email)).thenReturn(3L);

        assertThrows(InValidOtpException.class, () -> userService.verifyOtp(wrongOtp, email));

        verify(valueOperations, times(1)).set(eq("otp_locked:" + email), eq("locked"), any());
    }

    @Test
    void whenUserVerifiesOtp_thenRoleIsDerivedFromDatabaseNotClientDto() {
        String email = "victim@example.com";
        String validOtp = "123456";

        UserEntity storedUser = UserEntity.builder()
                .id("user-uuid-123")
                .email(email)
                .firstName("Alice")
                .lastName("Smith")
                .roles(Role.USER) // Stored as USER
                .status(UserStatus.ACTIVE)
                .build();

        when(redisTemplate.hasKey("otp_locked:" + email)).thenReturn(false);
        when(valueOperations.get(email)).thenReturn(validOtp);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(storedUser));
        when(jwtService.generateToken(email, "USER")).thenReturn("mock-valid-jwt-token");

        // Client attempts to escalate role to ADMIN in request DTO
        OtpVerificationDto dto = OtpVerificationDto.builder()
                .email(email)
                .otp(validOtp)
                .role(Role.ADMIN) // Attacker passes ADMIN
                .build();

        var response = userService.getLoginResponseDto(dto);

        assertNotNull(response);
        assertEquals(Role.USER, response.getRole(), "Role must be USER from database, not client-supplied ADMIN");
        verify(jwtService).generateToken(email, "USER");
    }
}
