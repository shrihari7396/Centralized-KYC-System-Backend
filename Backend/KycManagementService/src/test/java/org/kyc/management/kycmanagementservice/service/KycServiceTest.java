package org.kyc.management.kycmanagementservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kyc.management.kycmanagementservice.dto.DigilockerDataRequest;
import org.kyc.management.kycmanagementservice.dto.KycTokenResponseDto;
import org.kyc.management.kycmanagementservice.dto.UserProfileResponseDto;
import org.kyc.management.kycmanagementservice.entity.Address;
import org.kyc.management.kycmanagementservice.entity.KycEntity;
import org.kyc.management.kycmanagementservice.entity.KycRecord;
import org.kyc.management.kycmanagementservice.repository.KycRecordRepository;
import org.kyc.management.kycmanagementservice.repository.KycRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KycServiceTest {

    @Mock private KycRepository kycRepository;
    @Mock private KycRecordRepository kycRecordRepository;
    @Mock private KycTokenRedisService kycTokenRedisService;
    @Mock private JwtParserUtil jwtParserUtil;
    @Mock private TokenBlacklistService tokenBlacklistService;

    @InjectMocks private KycService kycService;

    @Test
    void storeDigilockerData_shouldPersistPendingEntity() {
        Address address =
                Address.builder()
                        .street("Street 1")
                        .city("Pune")
                        .state("MH")
                        .zipCode("411001")
                        .country("IN")
                        .build();
        DigilockerDataRequest request =
                DigilockerDataRequest.builder()
                        .userId("user-1")
                        .aadhaarNo("123456789012")
                        .pan("ABCDE1234F")
                        .drivingLicense("DL-123456")
                        .address(address)
                        .build();

        when(kycRepository.save(any(KycEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        KycEntity saved = kycService.storeDigilockerData(request);

        ArgumentCaptor<KycEntity> captor = ArgumentCaptor.forClass(KycEntity.class);
        verify(kycRepository).save(captor.capture());
        KycEntity persisted = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo("PENDING");
        assertThat(persisted.getUserId()).isEqualTo("user-1");
        assertThat(persisted.getAadhaarNo()).isEqualTo("123456789012");
        assertThat(persisted.getPan()).isEqualTo("ABCDE1234F");
        assertThat(persisted.getDrivingLicense()).isEqualTo("DL-123456");
        assertThat(persisted.getAddress()).isEqualTo(address);
        assertThat(persisted.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void getVerifiedDataForBank_shouldThrowWhenTokenExpired() {
        when(jwtParserUtil.isTokenExpired("token")).thenReturn(true);

        assertThatThrownBy(() -> kycService.getVerifiedDataForBank("token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void getVerifiedDataForBank_shouldThrowWhenUserIdMissing() {
        when(jwtParserUtil.isTokenExpired("token")).thenReturn(false);
        when(jwtParserUtil.extractJti("token")).thenReturn(null);
        when(jwtParserUtil.extractUserId("token")).thenReturn(" ");

        assertThatThrownBy(() -> kycService.getVerifiedDataForBank("token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID is missing");
    }

    @Test
    void getVerifiedDataForBank_shouldThrowWhenStatusNotVerified() {
        when(jwtParserUtil.isTokenExpired("token")).thenReturn(false);
        when(jwtParserUtil.extractJti("token")).thenReturn(null);
        when(jwtParserUtil.extractUserId("token")).thenReturn("user-1");
        when(kycRepository.findById("user-1"))
                .thenReturn(
                        Optional.of(
                                KycEntity.builder().userId("user-1").status("PENDING").build()));

        assertThatThrownBy(() -> kycService.getVerifiedDataForBank("token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not verified");
    }

    @Test
    void getVerifiedDataForBank_shouldReturnFilteredFieldsAndBlacklistToken() {
        Address address = Address.builder().street("Main").city("Delhi").build();
        KycEntity entity =
                KycEntity.builder()
                        .userId("user-1")
                        .aadhaarNo("123456789012")
                        .pan("ABCDE1234F")
                        .drivingLicense("DL-123456")
                        .address(address)
                        .status("VERIFIED")
                        .build();

        when(jwtParserUtil.isTokenExpired("token")).thenReturn(false);
        when(jwtParserUtil.extractJti("token")).thenReturn("jti-1");
        when(tokenBlacklistService.isBlacklisted("jti-1")).thenReturn(false);
        when(jwtParserUtil.extractUserId("token")).thenReturn("user-1");
        when(kycRepository.findById("user-1")).thenReturn(Optional.of(entity));
        when(jwtParserUtil.extractRequestedFields("token"))
                .thenReturn(Arrays.asList("userId", "pan", "address"));
        when(jwtParserUtil.getExpirationRemainingSeconds("token")).thenReturn(120L);

        Map<String, Object> response = kycService.getVerifiedDataForBank("token");

        assertThat(response)
                .containsEntry("userId", "user-1")
                .containsEntry("pan", "ABCDE1234F")
                .containsEntry("address", address)
                .containsEntry("status", "VERIFIED");
        assertThat(response).doesNotContainKey("aadhaarNo");

        verify(tokenBlacklistService).blacklistToken("jti-1", 120L);
    }

    @Test
    void getUserProfile_shouldIncludeVerifiedIdentityWhenRecordExists() {
        KycEntity entity =
                KycEntity.builder().userId("user-1").aadhaarNo("AAD").status("OK").build();
        KycRecord record =
                KycRecord.builder()
                        .firstName("Alex")
                        .lastName("Doe")
                        .dob(LocalDate.of(1990, 1, 1))
                        .build();

        when(kycRepository.findById("user-1")).thenReturn(Optional.of(entity));
        when(kycRecordRepository.findByAadharNumber("AAD")).thenReturn(Optional.of(record));

        UserProfileResponseDto response = kycService.getUserProfile("user-1");

        assertThat(response.getStatus()).isEqualTo("OK");
        assertThat(response.getIdentity().getFullName()).contains("Alex").contains("Doe");
        assertThat(response.getIdentity().getDob()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(response.getIdentity().getGender()).isEqualTo("Not Available");
    }

    @Test
    void getDocumentProofs_shouldMaskIdsAndReturnAllDocs() {
        KycEntity entity =
                KycEntity.builder()
                        .userId("user-1")
                        .aadhaarNo("123456789012")
                        .pan("ABCDE1234F")
                        .drivingLicense("DL-987654")
                        .build();
        KycRecord record =
                KycRecord.builder().updatedAt(LocalDateTime.of(2024, 1, 1, 10, 0)).build();

        when(kycRepository.findById("user-1")).thenReturn(Optional.of(entity));
        when(kycRecordRepository.findByAadharNumber("123456789012"))
                .thenReturn(Optional.of(record));

        var proofs = kycService.getDocumentProofs("user-1");

        assertThat(proofs).hasSize(3);
        assertThat(proofs.get(0).getMaskedId()).endsWith("9012");
        assertThat(proofs.get(1).getMaskedId()).endsWith("234F");
        assertThat(proofs.get(2).getMaskedId()).endsWith("7654");
        assertThat(proofs.get(0).getVerifiedAt()).isEqualTo(LocalDateTime.of(2024, 1, 1, 10, 0));
    }

    @Test
    void getKycToken_shouldReturnCachedTokenWhenPresent() {
        KycTokenResponseDto token =
                KycTokenResponseDto.builder().tokenId("token-1").status("ACTIVE").build();
        when(kycTokenRedisService.getToken("user-1")).thenReturn(Optional.of(token));

        KycTokenResponseDto response = kycService.getKycToken("user-1");

        assertThat(response.getTokenId()).isEqualTo("token-1");
        verify(kycTokenRedisService, never()).saveToken(eq("user-1"), any());
    }

    @Test
    void getKycToken_shouldGenerateNewTokenWhenAbsent() {
        when(kycTokenRedisService.getToken("user-1")).thenReturn(Optional.empty());

        KycTokenResponseDto response = kycService.getKycToken("user-1");

        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getAllowedData()).containsExactly("NAME", "DOB", "ADDRESS");
        verify(kycTokenRedisService, times(1)).saveToken(eq("user-1"), any());
    }

    @Test
    void revokeKycToken_shouldDelegateToRedisService() {
        kycService.revokeKycToken("user-1");

        verify(kycTokenRedisService).revokeToken("user-1");
    }
}
