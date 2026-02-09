package org.kyc.digilockerService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kyc.digilockerService.config.DigilockerConfig;
import org.kyc.digilockerService.model.KycStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DigilockerServiceImpl implements DigilockerService {

    private final DigilockerConfig digilockerConfig;
    private final RestTemplate restTemplate = new RestTemplate(); // Ideally configured as a bean
    private final java.util.Map<String, String> verifierStore = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public String getAuthorizationUrl(String state) {
        String codeVerifier = generateCodeVerifier();
        verifierStore.put(state, codeVerifier);
        log.info("Stored Code Verifier for state {}: {}", state, codeVerifier);

        String codeChallenge = generateCodeChallenge(codeVerifier);

        return String.format(
                "%s?response_type=code&client_id=%s&redirect_uri=%s&state=%s&code_challenge=%s&code_challenge_method=S256",
                digilockerConfig.getAuthUrl(),
                digilockerConfig.getClientId(),
                digilockerConfig.getRedirectUri(),
                state,
                codeChallenge);
    }

    @Override
    public String getAccessToken(String code, String state) {
        String codeVerifier = verifierStore.remove(state);
        if (codeVerifier == null) {
            throw new IllegalArgumentException("Invalid state or session expired");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("code", code);
        map.add("grant_type", "authorization_code");
        map.add("client_id", digilockerConfig.getClientId());
        map.add("client_secret", digilockerConfig.getClientSecret());
        map.add("redirect_uri", digilockerConfig.getRedirectUri());
        map.add("code_verifier", codeVerifier);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(digilockerConfig.getTokenUrl(), request,
                String.class);
        return response.getBody(); // Should parse this to get access_token
    }

    @Override
    public KycStatus handleCallback(String code, String state) {

        // 1. Exchange code for access token
        String tokenResponse = getAccessToken(code, state);

        // In real life parse access_token from JSON
        // For mock/demo we assume success
        String digilockerUserId = UUID.randomUUID().toString();

        // 2. Mark KYC as pending (verification will follow)
        KycStatus status = KycStatus.builder()
                .kycStatusId(UUID.randomUUID())
                .userId(UUID.fromString(digilockerUserId))
                .status("PENDING")
                .verifiedAt(null)
                .build();

        log.info("KYC initiated for user {}", digilockerUserId);

        return status;
    }

    @Override
    public Object getUserDetails(String accessToken) {
        HttpHeaders headers = createAuthHeaders(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Object> response = restTemplate.exchange(
                digilockerConfig.getUserUrl(),
                org.springframework.http.HttpMethod.GET,
                entity,
                Object.class);
        return response.getBody();
    }

    @Override
    public Object getIssuedDocuments(String accessToken) {
        HttpHeaders headers = createAuthHeaders(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        // Assuming api-base-url is configured as
        // https://digilocker.meripehchaan.gov.in/public/oauth2/2/files
        String url = digilockerConfig.getApiBaseUrl() + "/issued";

        ResponseEntity<Object> response = restTemplate.exchange(
                url,
                org.springframework.http.HttpMethod.GET,
                entity,
                Object.class);
        return response.getBody();
    }

    @Override
    public byte[] getFileFromUri(String accessToken, String uri) {
        HttpHeaders headers = createAuthHeaders(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        // API format: /public/oauth2/1/file/uri/{uri}
        // Note: The structure in config usually points to base, so let's construct it
        // carefully.
        // If config.apiBaseUrl is /public/oauth2/1/files, then this might be different.
        // Let's assume absolute paths or relative construction based on standard
        // DigiLocker URLs.
        // For /file/uri, it's slightly different root than /files/issued often.
        // Let's hardcode the standard path relative to domain for safety or add a new
        // config property if strict.
        // Using config.getApiBaseUrl() might be risky if it's specific to "files".
        // Let's assume a generic base like
        // https://digilocker.meripehchaan.gov.in/public/oauth2/1

        String url = "https://digilocker.meripehchaan.gov.in/public/oauth2/1/file/" + uri;

        ResponseEntity<byte[]> response = restTemplate.exchange(
                url,
                org.springframework.http.HttpMethod.GET,
                entity,
                byte[].class);
        return response.getBody();
    }

    @Override
    public Object pullDocument(String accessToken, Object pullRequest) {
        HttpHeaders headers = createAuthHeaders(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(pullRequest, headers);

        String url = "https://digilocker.meripehchaan.gov.in/public/oauth2/1/pull/pulldocument";

        ResponseEntity<Object> response = restTemplate.postForEntity(url, entity, Object.class);
        return response.getBody();
    }

    private HttpHeaders createAuthHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }

    private String generateCodeVerifier() {
        // High-entropy cryptographic random string
        // Minimum length of 43 characters and a maximum length of 128 characters
        return UUID.randomUUID().toString() + UUID.randomUUID().toString();
    }

    private String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
