package org.kyc.digilockerService.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kyc.digilockerService.config.DigilockerConfig;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DigilockerServiceImplTest {

    @Mock
    private DigilockerConfig digilockerConfig;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private DigilockerServiceImpl digilockerService;

    @BeforeEach
    void setUp() throws Exception {
        // Inject the mocked RestTemplate into the service using reflection because it's
        // initialized in the field
        Field restTemplateField = DigilockerServiceImpl.class.getDeclaredField("restTemplate");
        restTemplateField.setAccessible(true);
        restTemplateField.set(digilockerService, restTemplate);

        // Setup mock config
        lenient().when(digilockerConfig.getAuthUrl()).thenReturn("https://auth.url");
        lenient().when(digilockerConfig.getClientId()).thenReturn("client-id");
        lenient().when(digilockerConfig.getRedirectUri()).thenReturn("https://redirect.uri");
        lenient().when(digilockerConfig.getTokenUrl()).thenReturn("https://token.url");
        lenient().when(digilockerConfig.getApiBaseUrl()).thenReturn("https://api.url");
        lenient().when(digilockerConfig.getClientSecret()).thenReturn("secret");
    }

    @Test
    void getAuthorizationUrl_ShouldReturnCorrectUrlAndStoreVerifier() {
        String state = "test-state";
        String url = digilockerService.getAuthorizationUrl(state);

        assertNotNull(url);
        assertTrue(url.contains("response_type=code"));
        assertTrue(url.contains("client_id=client-id"));
        assertTrue(url.contains("state=test-state"));
        assertTrue(url.contains("code_challenge="));
    }

    @Test
    void getAccessToken_ShouldCallTokenEndpointWithCorrectParams() {
        String state = "test-state";
        String code = "auth-code";

        // First generate auth url to store the verifier
        digilockerService.getAuthorizationUrl(state);

        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"access_token\":\"token\"}"));

        digilockerService.getAccessToken(code, state);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq("https://token.url"), captor.capture(), eq(String.class));

        HttpEntity entity = captor.getValue();
        // Check body content if possible, simplistic check here
        assertNotNull(entity.getBody());
    }

    @Test
    void getIssuedDocuments_ShouldCallApiWithBearerToken() {
        String accessToken = "test-token";

        when(restTemplate.exchange(any(String.class), any(), any(HttpEntity.class), eq(Object.class)))
                .thenReturn(ResponseEntity.ok(new Object()));

        digilockerService.getIssuedDocuments(accessToken);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(eq("https://api.url/issued"), any(), captor.capture(), eq(Object.class));

        HttpEntity entity = captor.getValue();
        assertTrue(entity.getHeaders().get("Authorization").get(0).startsWith("Bearer test-token"));
    }
}
