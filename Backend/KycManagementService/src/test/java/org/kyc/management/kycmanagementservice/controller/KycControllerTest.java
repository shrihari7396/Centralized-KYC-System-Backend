package org.kyc.management.kycmanagementservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.kyc.management.kycmanagementservice.dto.DigilockerDataRequest;
import org.kyc.management.kycmanagementservice.dto.DocumentProofResponseDto;
import org.kyc.management.kycmanagementservice.dto.KycTokenResponseDto;
import org.kyc.management.kycmanagementservice.dto.UserProfileResponseDto;
import org.kyc.management.kycmanagementservice.dto.VerifiedIdentity;
import org.kyc.management.kycmanagementservice.entity.Address;
import org.kyc.management.kycmanagementservice.entity.KycEntity;
import org.kyc.management.kycmanagementservice.service.KycService;

@WebMvcTest(KycController.class)
class KycControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private KycService kycService;

    @Test
    void storeDigilockerData_shouldReturnOk() throws Exception {
        Address address = Address.builder().street("Street 1").city("Pune").build();
        DigilockerDataRequest request =
                DigilockerDataRequest.builder()
                        .userId("user-1")
                        .aadhaarNo("123456789012")
                        .pan("ABCDE1234F")
                        .drivingLicense("DL-123456")
                        .address(address)
                        .build();
        KycEntity responseEntity =
                KycEntity.builder().userId("user-1").status("PENDING").address(address).build();

        when(kycService.storeDigilockerData(any(DigilockerDataRequest.class)))
                .thenReturn(responseEntity);

        mockMvc.perform(
                        post("/api/kyc/digilocker")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.address.city").value("Pune"));
    }

    @Test
    void getDocumentsForBank_shouldReturnOk() throws Exception {
        when(kycService.getVerifiedDataForBank("Bearer token"))
                .thenReturn(Map.of("userId", "user-1", "status", "VERIFIED"));

        mockMvc.perform(get("/api/kyc/bank/documents").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.status").value("VERIFIED"));
    }

    @Test
    void getDocumentsForBank_shouldReturnBadRequestOnIllegalArgument() throws Exception {
        when(kycService.getVerifiedDataForBank("Bearer token"))
                .thenThrow(new IllegalArgumentException("Invalid JWT"));

        mockMvc.perform(get("/api/kyc/bank/documents").header("Authorization", "Bearer token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Invalid JWT"));
    }

    @Test
    void getDocumentsForBank_shouldReturnForbiddenOnRuntime() throws Exception {
        when(kycService.getVerifiedDataForBank("Bearer token"))
                .thenThrow(new RuntimeException("Forbidden"));

        mockMvc.perform(get("/api/kyc/bank/documents").header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$").value("Forbidden"));
    }

    @Test
    void getUserProfile_shouldReturnOk() throws Exception {
        Address address = Address.builder().city("Mumbai").build();
        UserProfileResponseDto profile =
                UserProfileResponseDto.builder()
                        .status("VERIFIED")
                        .identity(
                                VerifiedIdentity.builder()
                                        .fullName("Alex Doe")
                                        .dob(LocalDate.of(1990, 1, 1))
                                        .gender("Not Available")
                                        .build())
                        .address(address)
                        .build();

        when(kycService.getUserProfile("user-1")).thenReturn(profile);

        mockMvc.perform(get("/api/kyc/user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"))
                .andExpect(jsonPath("$.identity.fullName").value("Alex Doe"))
                .andExpect(jsonPath("$.address.city").value("Mumbai"));
    }

    @Test
    void getUserProfile_shouldReturnNotFound() throws Exception {
        when(kycService.getUserProfile("user-1")).thenThrow(new RuntimeException("Not found"));

        mockMvc.perform(get("/api/kyc/user-1")).andExpect(status().isNotFound());
    }

    @Test
    void getDocumentProofs_shouldReturnOk() throws Exception {
        List<DocumentProofResponseDto> proofs =
                List.of(
                        new DocumentProofResponseDto(
                                "AADHAAR", "UIDAI", "XXXX-XXXX-9012", LocalDateTime.now()));
        when(kycService.getDocumentProofs("user-1")).thenReturn(proofs);

        mockMvc.perform(get("/api/kyc/user-1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentType").value("AADHAAR"))
                .andExpect(jsonPath("$[0].maskedId").value("XXXX-XXXX-9012"));
    }

    @Test
    void getDocumentProofs_shouldReturnNotFound() throws Exception {
        when(kycService.getDocumentProofs("user-1")).thenThrow(new RuntimeException("Not found"));

        mockMvc.perform(get("/api/kyc/user-1/documents")).andExpect(status().isNotFound());
    }

    @Test
    void getKycToken_shouldReturnOk() throws Exception {
        KycTokenResponseDto token =
                KycTokenResponseDto.builder().tokenId("token-1").status("ACTIVE").build();
        when(kycService.getKycToken("user-1")).thenReturn(token);

        mockMvc.perform(get("/api/kyc/user-1/token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenId").value("token-1"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getKycToken_shouldReturnServerError() throws Exception {
        when(kycService.getKycToken("user-1")).thenThrow(new RuntimeException("error"));

        mockMvc.perform(get("/api/kyc/user-1/token")).andExpect(status().isInternalServerError());
    }

    @Test
    void revokeKycToken_shouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/kyc/user-1/token/revoke")).andExpect(status().isOk());
    }

    @Test
    void revokeKycToken_shouldReturnServerError() throws Exception {
        doThrow(new RuntimeException("error")).when(kycService).revokeKycToken(eq("user-1"));

        mockMvc.perform(post("/api/kyc/user-1/token/revoke"))
                .andExpect(status().isInternalServerError());
    }
}
