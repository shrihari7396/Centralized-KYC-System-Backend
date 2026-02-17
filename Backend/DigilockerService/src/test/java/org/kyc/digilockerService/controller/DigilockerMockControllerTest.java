package org.kyc.digilockerService.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.kyc.digilockerService.dto.OtpGenerateRequest;
import org.kyc.digilockerService.dto.OtpGenerateResponse;
import org.kyc.digilockerService.dto.OtpVerifyRequest;
import org.kyc.digilockerService.dto.OtpVerifyResponse;
import org.kyc.digilockerService.dto.DocumentResponse;
import org.kyc.digilockerService.model.DocumentType;
import org.kyc.digilockerService.service.DigilockerMockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.kyc.digilockerService.config.SecurityConfig;
import org.springframework.context.annotation.Import;

@WebMvcTest(DigilockerMockController.class)
@Import(SecurityConfig.class)
class DigilockerMockControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private DigilockerMockService service;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        void testGenerateOtp() throws Exception {
                OtpGenerateRequest request = new OtpGenerateRequest();
                request.setDocumentType(DocumentType.PAN);
                request.setDocumentNumber("ABCDE1234F");

                OtpGenerateResponse response = OtpGenerateResponse.builder()
                                .requestId("test-request-id")
                                .message("OTP sent successfully")
                                .build();

                when(service.generateOtp(any(OtpGenerateRequest.class))).thenReturn(response);

                mockMvc.perform(post("/digilocker/otp/generate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.requestId").value("test-request-id"))
                                .andExpect(jsonPath("$.message").value("OTP sent successfully"));
        }

        @Test
        void testVerifyOtp_Success() throws Exception {
                OtpVerifyRequest request = new OtpVerifyRequest();
                request.setRequestId("test-request-id");
                request.setOtp("123456");
                request.setDocumentNumber("ABCDE1234F");

                OtpVerifyResponse response = OtpVerifyResponse.builder()
                                .status("SUCCESS")
                                .verificationToken("valid-token")
                                .build();

                when(service.verifyOtp(any(OtpVerifyRequest.class))).thenReturn(response);

                mockMvc.perform(post("/digilocker/otp/verify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("SUCCESS"))
                                .andExpect(jsonPath("$.verificationToken").value("valid-token"));
        }

        @Test
        void testVerifyOtp_Failed() throws Exception {
                OtpVerifyRequest request = new OtpVerifyRequest();
                request.setRequestId("test-request-id");
                request.setOtp("000000");
                request.setDocumentNumber("ABCDE1234F");

                OtpVerifyResponse response = OtpVerifyResponse.builder()
                                .status("FAILED")
                                .reason("INVALID_OTP")
                                .build();

                when(service.verifyOtp(any(OtpVerifyRequest.class))).thenReturn(response);

                mockMvc.perform(post("/digilocker/otp/verify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("FAILED"))
                                .andExpect(jsonPath("$.reason").value("INVALID_OTP"));
        }

        @Test
        void testGetDocument() throws Exception {
                DocumentResponse response = DocumentResponse.builder()
                                .name("John Doe")
                                .gender("M")
                                .dob("1990-01-01")
                                .address("123 Street")
                                .documentNumber("XXXX1234")
                                .build();

                when(service.fetchDocument(eq("valid-token"), eq(DocumentType.AADHAAR), eq("123412341234")))
                                .thenReturn(response);

                mockMvc.perform(get("/digilocker/document")
                                .header("Authorization", "Bearer valid-token")
                                .param("documentType", "AADHAAR")
                                .param("documentNumber", "123412341234"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("John Doe"))
                                .andExpect(jsonPath("$.documentNumber").value("XXXX1234"));
        }

        @Test
        void testHealth() throws Exception {
                mockMvc.perform(get("/digilocker/health"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("UP"));
        }
}
