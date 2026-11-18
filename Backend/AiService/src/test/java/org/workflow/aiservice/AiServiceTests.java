package org.workflow.aiservice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.workflow.aiservice.GenAi.GroqApiService;
import org.workflow.aiservice.GenAi.GroqController;
import org.workflow.aiservice.GenAi.PromptDto;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiServiceTests {

    @Mock private GroqApiService groqApiService;
    @InjectMocks private GroqController groqController;

    @Test
    @DisplayName("GroqController invokes GroqApiService with prompt content")
    void testGroqControllerCallsService() {
        when(groqApiService.callGroqApi(anyString(), eq("What is centralized KYC?")))
                .thenReturn("Centralized KYC is a tokenized identity platform.");

        PromptDto prompt = new PromptDto();
        prompt.setContent("What is centralized KYC?");

        String response = groqController.callApi(prompt);
        assertNotNull(response);
        assertEquals("Centralized KYC is a tokenized identity platform.", response);
        verify(groqApiService, times(1)).callGroqApi(anyString(), eq("What is centralized KYC?"));
    }

    @Test
    @DisplayName("GroqApiService throws IllegalStateException when GROQ_API_KEY is not configured")
    void testGroqApiServiceWithoutApiKeyThrows() {
        RestTemplateBuilder builder = new RestTemplateBuilder();
        GroqApiService service = new GroqApiService(builder, 1000, 1000);

        assertThrows(IllegalStateException.class, () ->
                service.callGroqApi("system", "hello"));
    }
}
