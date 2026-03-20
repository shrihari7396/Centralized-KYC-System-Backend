package org.kyc.management.kycmanagementservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class JwtParserUtil {

  private final ObjectMapper objectMapper = new ObjectMapper();

  /** Extracts the complete JSON payload from the given JWT token. */
  public JsonNode parsePayload(String token) {
    try {
      if (token != null && token.startsWith("Bearer ")) {
        token = token.substring(7);
      }
      String[] chunks = token.split("\\.");
      if (chunks.length < 2) {
        throw new IllegalArgumentException("Invalid JWT token format");
      }

      Base64.Decoder decoder = Base64.getUrlDecoder();
      String payload = new String(decoder.decode(chunks[1]));
      return objectMapper.readTree(payload);
    } catch (Exception e) {
      throw new RuntimeException("Error parsing JWT Token", e);
    }
  }

  /** Utility method to extract a specific claim as a string. */
  public String extractUserId(String token) {
    JsonNode payload = parsePayload(token);
    if (payload.has("sub")) {
      return payload.get("sub").asText();
    }
    return null; // or throw exception depending on security model
  }

  /** Extracts the requested fields list from the token. */
  public List<String> extractRequestedFields(String token) {
    JsonNode payload = parsePayload(token);
    List<String> fields = new ArrayList<>();
    if (payload.has("documentFields") && payload.get("documentFields").isArray()) {
      for (JsonNode fieldNode : payload.get("documentFields")) {
        fields.add(fieldNode.asText());
      }
    }
    return fields;
  }

  /** Extracts the token ID (JTI). */
  public String extractJti(String token) {
    JsonNode payload = parsePayload(token);
    if (payload.has("jti")) {
      return payload.get("jti").asText();
    }
    return null;
  }

  /** Checks if the token is expired by verifying the 'exp' claim. */
  public boolean isTokenExpired(String token) {
    JsonNode payload = parsePayload(token);
    if (payload.has("exp")) {
      long expEpoch = payload.get("exp").asLong();
      long currentEpoch = System.currentTimeMillis() / 1000;
      return currentEpoch >= expEpoch;
    }
    // If there's no expiration claim, we could either consider it never expires,
    // or immediately expires. Let's assume valid but not trackable for expiration.
    return false;
  }

  /** Extracts the remaining time to live for a token in seconds. */
  public long getExpirationRemainingSeconds(String token) {
    JsonNode payload = parsePayload(token);
    if (payload.has("exp")) {
      long expEpoch = payload.get("exp").asLong();
      long currentEpoch = System.currentTimeMillis() / 1000;
      long remaining = expEpoch - currentEpoch;
      return remaining > 0 ? remaining : 0;
    }
    return 0;
  }
}
