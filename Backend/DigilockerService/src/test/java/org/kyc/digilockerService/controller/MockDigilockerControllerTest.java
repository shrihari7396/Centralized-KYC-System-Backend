package org.kyc.digilockerService.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.kyc.digilockerService.audit.AuditPublisher;
import org.kyc.digilockerService.model.ConsentRequest;
import org.kyc.digilockerService.service.MockDigilockerService;
import org.kyc.digilockerService.service.MockDigilockerServiceImpl;
import org.kyc.digilockerService.util.DataGeneratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DigilockerMockController.class)
@Import(org.kyc.digilockerService.config.SecurityConfig.class)
public class MockDigilockerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public MockDigilockerService mockDigilockerService() {
            // Use the real implementation for the mock service test to verify logic +
            // controller
            return new MockDigilockerServiceImpl(new AuditPublisher());
        }
    }

    @Test
    public void testConsentFlow() throws Exception {
        ConsentRequest request = new ConsentRequest();
        request.setPurpose("KYC");
        request.setRequestedBy("TestApp");

        mockMvc.perform(post("/digilocker/consent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("GRANTED"))
                .andExpect(jsonPath("$.consentId").isNotEmpty());
    }

    @Test
    public void testGetUserProfile() throws Exception {
        String userId = DataGeneratorUtil.generateUuid();

        mockMvc.perform(get("/digilocker/user/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.digilockerUserId").value(userId))
                .andExpect(jsonPath("$.name").isNotEmpty())
                .andExpect(jsonPath("$.aadhaar").isNotEmpty());
    }

    @Test
    public void testGetDocuments() throws Exception {
        String userId = DataGeneratorUtil.generateUuid();

        mockMvc.perform(get("/digilocker/documents/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documents").isArray())
                .andExpect(jsonPath("$.documents[0].type").value("AADHAAR"))
                .andExpect(jsonPath("$.documents[0].documentHash").isNotEmpty());
    }
}
