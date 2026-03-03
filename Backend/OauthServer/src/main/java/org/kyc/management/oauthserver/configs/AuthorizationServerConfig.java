package org.kyc.management.oauthserver.configs;

import java.util.UUID;

import javax.sql.DataSource;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;

/**
 * Configuration for the OAuth2 Authorization Server.
 * <p>
 * This class defines the security filter chain for the authorization server endpoints
 * and configures the core components like client repository, authorization service, 
 * token signing keys, and general server settings.
 * </p>
 */
@Configuration
public class AuthorizationServerConfig {

	/**
	 * Configures the Security Filter Chain for the Authorization Server.
	 * 
	 * @param http the HttpSecurity builder
	 * @return the configured SecurityFilterChain
	 * @throws Exception if an error occurs during configuration
	 */
	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
		// Apply the default security configurations for an OAuth2 Authorization Server
		// This sets up the endpoints for authorization, token, jwks, revocation, etc.
		OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
		
		// Configure OpenID Connect (OIDC) 1.0 support.
		// Requires importing the correct OAuth2AuthorizationServerConfigurer.
		http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
			.oidc(Customizer.withDefaults());	// Enable OpenID Connect 1.0
		
		http
			// Redirect to the /login page if the user is not authenticated and tries to access
			// the authorization endpoint.
			.exceptionHandling((exceptions) -> exceptions
				.authenticationEntryPoint(
					new LoginUrlAuthenticationEntryPoint("/login"))
			)
			// Configure the OAuth2 Resource Server to accept JWTs.
			// This is needed so that the User Info endpoint and Client Registration endpoint
			// can be accessed using access tokens.
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

		return http.build();
	}

	/**
	 * Configures the RegisteredClientRepository to manage clients.
	 * Uses JdbcRegisteredClientRepository to persist clients in the database.
	 * 
	 * @param jdbcTemplate the JdbcTemplate for database interactions
	 * @return the RegisteredClientRepository
	 */
	@Bean
	public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
		JdbcRegisteredClientRepository registeredClientRepository = new JdbcRegisteredClientRepository(jdbcTemplate);
        //It stores OAuth clients, not users. eg mobiledevice , frontend
		// Initialize with a default 'oidc-client' if it doesn't exist.
		// This is useful for testing and initial setup.
		try {
		    if (registeredClientRepository.findByClientId("oidc-client") == null) {
		        RegisteredClient oidcClient = RegisteredClient.withId(UUID.randomUUID().toString())
		                .clientId("oidc-client")
		                .clientSecret("{noop}secret")
		                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
		                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
		                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
		                .redirectUri("http://127.0.0.1:8080/login/oauth2/code/oidc-client")
		                .postLogoutRedirectUri("http://127.0.0.1:8080/")
		                .scope(OidcScopes.OPENID)
		                .scope(OidcScopes.PROFILE)
		                .scope("read")
		                .scope("write")
		                // Require user consent page during authorization flow
		                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
		                .build();
		        registeredClientRepository.save(oidcClient);
		    }
        } catch (Exception e) {
            // Ignore exceptions during initialization (e.g., if the table doesn't exist yet)
        }
        
		return registeredClientRepository;
	}

	/**
	 * Configures the OAuth2AuthorizationService to manage authorizations.
	 * Persists authorization data (access tokens, codes, etc.) in the database.
	 */
	@Bean
	public OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
		return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
	}

	/**
	 * Configures the OAuth2AuthorizationConsentService to manage user consents.
	 * Persists consent data in the database.
	 */
	@Bean
	public OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
		return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
	}

	/**
	 * Configures the JWKSource for signing access tokens.
	 * Generates a new RSA key pair on startup (in this example).
	 * In a production environment, you should load this from a KeyStore.
	 */
	@Bean
	public JWKSource<SecurityContext> jwkSource() {
		RSAKey rsaKey = Jwks.generateRsa();
		JWKSet jwkSet = new JWKSet(rsaKey);
		return new ImmutableJWKSet<>(jwkSet);
	}

	/**
	 * Configures the JwtDecoder for decoding signed access tokens.
	 * Used by the Resource Server configuration.
	 */
	@Bean
	public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
		return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
	}

	/**
	 * Configures global settings for the Authorization Server.
	 */
	@Bean
	public AuthorizationServerSettings authorizationServerSettings() {
		return AuthorizationServerSettings.builder().build();
	}

}
