package org.rbi.kycmanagementservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JwtParserUtilTest {

    private final JwtParserUtil jwtParserUtil = new JwtParserUtil();

    @Test
    void shouldExtractClaimsFromBearerToken() throws Exception {
        long exp = Instant.now().plusSeconds(3600).getEpochSecond();
        String token =
                buildToken(
                        Map.of(
                                "sub",
                                "user-1",
                                "jti",
                                "jti-1",
                                "exp",
                                exp,
                                "documentFields",
                                List.of("userId", "pan")));

        assertThat(jwtParserUtil.extractUserId("Bearer " + token)).isEqualTo("user-1");
        assertThat(jwtParserUtil.extractJti(token)).isEqualTo("jti-1");
        assertThat(jwtParserUtil.extractRequestedFields(token)).containsExactly("userId", "pan");
        assertThat(jwtParserUtil.isTokenExpired(token)).isFalse();
        assertThat(jwtParserUtil.getExpirationRemainingSeconds(token)).isGreaterThan(0);
    }

    @Test
    void shouldDetectExpiredToken() throws Exception {
        long exp = Instant.now().minusSeconds(10).getEpochSecond();
        String token = buildToken(Map.of("sub", "user-1", "exp", exp));

        assertThat(jwtParserUtil.isTokenExpired(token)).isTrue();
        assertThat(jwtParserUtil.getExpirationRemainingSeconds(token)).isEqualTo(0);
    }

    @Test
    void parsePayload_shouldThrowOnInvalidToken() {
        assertThatThrownBy(() -> jwtParserUtil.parsePayload("invalid"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error parsing JWT");
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
