package org.kyc.management.bankservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/bank/kyc/request-otp",
                                                "/bank/kyc/otp-verification")
                                        .permitAll()
                                        .requestMatchers("/bank/kyc/initiate", "/bank/kyc/submit")
                                        .hasAuthority("BANK")
                                        .requestMatchers("/bank/kyc/bypass")
                                        .hasAuthority("ADMIN")
                                        .requestMatchers("/bank/kyc/status/**")
                                        .hasAnyAuthority("BANK", "ADMIN")
                                        .anyRequest()
                                        .authenticated())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
