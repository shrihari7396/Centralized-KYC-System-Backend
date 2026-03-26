package org.rbi.kycmanagementservice.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.rbi.kycmanagementservice.dto.DigilockerDataRequest;
import org.rbi.kycmanagementservice.entity.Address;
import org.rbi.kycmanagementservice.entity.KycEntity;
import org.rbi.kycmanagementservice.entity.KycRecord;
import org.rbi.kycmanagementservice.repository.KycRecordRepository;
import org.rbi.kycmanagementservice.repository.KycRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KycManagementServiceIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private KycRepository kycRepository;
    @Autowired private KycRecordRepository kycRecordRepository;

    @Test
    void createFetchDocumentsFlow_shouldPersistAndReturnData() throws Exception {
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

        mockMvc.perform(
                        post("/api/kyc/digilocker")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        KycEntity saved = kycRepository.findById("user-1").orElseThrow();
        assertThat(saved.getAadhaarNo()).isEqualTo("123456789012");
        assertThat(saved.getStatus()).isEqualTo("PENDING");

        KycRecord record =
                KycRecord.builder()
                        .mobileNumber("9999999999")
                        .aadharNumber("123456789012")
                        .panNumber("ABCDE1234F")
                        .firstName("Alex")
                        .lastName("Doe")
                        .dob(LocalDate.of(1990, 1, 1))
                        .address("Address Line")
                        .status("VERIFIED")
                        .build();
        kycRecordRepository.save(record);

        mockMvc.perform(get("/api/kyc/user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.identity.fullName").value("Alex Doe"))
                .andExpect(jsonPath("$.identity.dob").value("1990-01-01"));

        mockMvc.perform(get("/api/kyc/user-1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].maskedId").value("XXXX-XXXX-9012"));
    }

    @Test
    void getDocumentsForBank_shouldReturnFilteredFields() throws Exception {
        Address address = Address.builder().street("Main").city("Delhi").build();
        KycEntity entity =
                KycEntity.builder()
                        .userId("user-2")
                        .aadhaarNo("999988887777")
                        .pan("ABCDE1234F")
                        .drivingLicense("DL-000111")
                        .address(address)
                        .status("VERIFIED")
                        .build();
        kycRepository.save(entity);

        String token =
                "Bearer "
                        + buildToken(
                                Map.of(
                                        "sub",
                                        "user-2",
                                        "documentFields",
                                        List.of("userId", "pan", "address")));

        mockMvc.perform(get("/api/kyc/bank/documents").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-2"))
                .andExpect(jsonPath("$.pan").value("ABCDE1234F"))
                .andExpect(jsonPath("$.address.city").value("Delhi"))
                .andExpect(jsonPath("$.status").value("VERIFIED"));
    }

    private String buildToken(Map<String, Object> payload) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String headerJson = mapper.writeValueAsString(Map.of("alg", "none", "typ", "JWT"));
        String payloadJson = mapper.writeValueAsString(payload);

        String header =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String body =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signature = "signature";

        return header + "." + body + "." + signature;
    }
}
