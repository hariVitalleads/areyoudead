package com.checkin.controller;

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
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/register", "/api/auth/login",
                                "/api/user/register", "/api/user/forgot-password", "/api/user/reset-password",
                                "/api/login/register", "/api/login/login", "/api/login/forgot-password", "/api/login/reset-password")
                        .permitAll()
                        .anyRequest().authenticated())
                .build();
    }
}
