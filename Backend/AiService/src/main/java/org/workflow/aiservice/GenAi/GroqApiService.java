package org.workflow.aiservice.GenAi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class GroqApiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${groq.api.key:}")
    private String apiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String apiUrl;

    @Value("${groq.api.model:llama-3.3-70b-versatile}")
    private String model;

    public GroqApiService(
            RestTemplateBuilder builder,
            @Value("${groq.api.connect-timeout-ms:5000}") int connectTimeout,
            @Value("${groq.api.read-timeout-ms:15000}") int readTimeout) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(connectTimeout))
                .setReadTimeout(Duration.ofMillis(readTimeout))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String callGroqApi(String systemPrompt, String userMessage) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GROQ_API_KEY is not configured. Set environment variable GROQ_API_KEY.");
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put(
                    "messages",
                    new Object[] {
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                    });

            String requestBodyJson = objectMapper.writeValueAsString(requestBody);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> requestEntity = new HttpEntity<>(requestBodyJson, headers);

            log.info("Dispatching query to LLM endpoint: {}, model: {}", apiUrl, model);
            ResponseEntity<String> response =
                    restTemplate.exchange(
                            apiUrl,
                            HttpMethod.POST,
                            requestEntity,
                            String.class);

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            return rootNode.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            log.error("Failed to query AI service at {}: {}", apiUrl, e.getMessage());
            throw new RuntimeException("AI inference invocation failed: " + e.getMessage(), e);
        }
    }
}
