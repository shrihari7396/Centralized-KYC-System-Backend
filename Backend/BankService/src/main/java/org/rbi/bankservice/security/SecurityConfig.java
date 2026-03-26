package org.rbi.bankservice.security;

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
                                                "/bank/kyc/otp/**",
                                                "/bank/kyc/banks",
                                                "/bank/kyc/banks/**",
                                                "/bank/kyc/register")
                                        .permitAll()
                                        .requestMatchers("/bank/kyc/bypass")
                                        .hasRole("ADMIN")
                                        .requestMatchers("/bank/kyc/status/**")
                                        .hasAnyRole("BANK", "BANK_AGENT", "ADMIN")
                                        .requestMatchers(
                                                "/bank/kyc/initiate",
                                                "/bank/kyc/bank/profile",
                                                "/bank/kyc/customers/**",
                                                "/bank/kyc/stats/**",
                                                "/bank/kyc/consents/**",
                                                "/bank/kyc/fetch/**",
                                                "/bank/kyc/history/**")
                                        .hasAnyRole("BANK", "BANK_AGENT")
                                        .requestMatchers("/bank/kyc/logout")
                                        .authenticated()
                                        .anyRequest()
                                        .authenticated())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
