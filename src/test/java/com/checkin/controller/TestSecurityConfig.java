package com.checkin.controller;

import com.checkin.config.RateLimitProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test-only security: permit all public API paths, require authentication for the rest.
 * Replaces the main SecurityConfig in controller tests so JwtAuthenticationFilter is not needed.
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    public RateLimitProperties rateLimitProperties() {
        RateLimitProperties p = new RateLimitProperties();
        p.setAuthRequestsPerWindow(100);
        p.setWindowSeconds(60);
        return p;
    }
    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/user/register", "/api/user/login", "/api/user/refresh",
                                "/api/user/forgot-password", "/api/user/reset-password",
                                "/api/user/resend-verification-email",
                                "/api/login/login")
                        .permitAll()
                        .requestMatchers("/api/admin/**").hasRole("SUPER_USER")
                        .anyRequest().authenticated())
                .build();
    }
}
