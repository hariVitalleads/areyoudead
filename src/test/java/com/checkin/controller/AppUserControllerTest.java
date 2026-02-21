package com.checkin.controller;

import com.checkin.dto.AppUserDetailsResponse;
import com.checkin.dto.ForgotPasswordRequest;
import com.checkin.dto.RegisterRequest;
import com.checkin.dto.ResetPasswordRequest;
import com.checkin.dto.UpdateAppUserRequest;
import com.checkin.dto.UserResponse;
import com.checkin.security.JwtService;
import com.checkin.security.UserPrincipal;
import com.checkin.service.AppUserService;
import com.checkin.service.AuthService;
import com.checkin.service.LoginService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import org.springframework.context.annotation.Import;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AppUserController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class AppUserControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private AuthService authService;

        @MockitoBean
        private LoginService loginService;

        @MockitoBean
        private AppUserService appUserService;

        /** Required by JwtAuthenticationFilter when security auto-config is loaded. */
        @MockitoBean
        private JwtService jwtService;

        @Autowired
        private ObjectMapper objectMapper;

        private UUID userId;
        private UserResponse userResponse;
        private AppUserDetailsResponse appUserDetailsResponse;

        @BeforeEach
        void setUp() {
                userId = UUID.randomUUID();
                userResponse = new UserResponse(userId, "test@example.com", Instant.now());
                appUserDetailsResponse = new AppUserDetailsResponse(
                                userId, "test@example.com", Instant.now(), Instant.now());
        }

        @AfterEach
        void tearDown() {
                SecurityContextHolder.clearContext();
        }

        @Test
        @WithAnonymousUser
        void register_Success() throws Exception {
                // Given
                RegisterRequest request = new RegisterRequest();
                request.setEmail("test@example.com");
                request.setPassword("password123");

                when(authService.register(any(RegisterRequest.class))).thenReturn(userResponse);

                // When & Then
                mockMvc.perform(post("/api/user/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(userId.toString()))
                                .andExpect(jsonPath("$.email").value("test@example.com"));
        }

        @Test
        @WithAnonymousUser
        void forgotPassword_Success() throws Exception {
                // Given
                ForgotPasswordRequest request = new ForgotPasswordRequest();
                request.setEmail("test@example.com");
                String token = "reset-token";

                when(loginService.forgotPassword(anyString())).thenReturn(token);

                // When & Then
                mockMvc.perform(post("/api/user/forgot-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                                .andExpect(status().isAccepted())
                                .andExpect(jsonPath("$.message")
                                                .value("If the account exists, a reset token has been issued."))
                                .andExpect(jsonPath("$.resetToken").value(token));
        }

        @Test
        @WithAnonymousUser
        void resetPassword_Success() throws Exception {
                // Given
                ResetPasswordRequest request = new ResetPasswordRequest();
                request.setToken("reset-token");
                request.setNewPassword("newPassword123");

                // When & Then
                mockMvc.perform(post("/api/user/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Password has been updated."));
        }

        @Test
        void me_Success() throws Exception {
                // Given: set SecurityContext so controller receives Authentication (addFilters
                // = false skips SecurityContextPersistenceFilter)
                Authentication auth = new UsernamePasswordAuthenticationToken(
                                new UserPrincipal(userId, "test@example.com"), null, null);
                when(appUserService.me(eq(userId))).thenReturn(appUserDetailsResponse);

                // When & Then
                mockMvc.perform(get("/api/user/me")
                                .with(authentication(auth)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(userId.toString()))
                                .andExpect(jsonPath("$.email").value("test@example.com"));
        }

        @Test
        void updateDetails_Success() throws Exception {
                // Given: set SecurityContext so controller receives Authentication
                Authentication auth = new UsernamePasswordAuthenticationToken(
                                new UserPrincipal(userId, "test@example.com"), null, null);
                UpdateAppUserRequest request = new UpdateAppUserRequest();
                request.setEmail("newemail@example.com");

                when(appUserService.update(eq(userId), eq("newemail@example.com")))
                                .thenReturn(appUserDetailsResponse);

                // When & Then
                mockMvc.perform(put("/api/user/details")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf())
                                .with(authentication(auth)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(userId.toString()));
        }

        @Test
        void updateDetails_InvalidRequest_ReturnsBadRequest() throws Exception {
                // Given: set SecurityContext so controller receives Authentication
                Authentication auth = new UsernamePasswordAuthenticationToken(
                                new UserPrincipal(userId, "test@example.com"), null, null);
                UpdateAppUserRequest request = new UpdateAppUserRequest();
                request.setEmail("invalid-email"); // Invalid email format

                // When & Then
                mockMvc.perform(put("/api/user/details")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf())
                                .with(authentication(auth)))
                                .andExpect(status().isBadRequest());
        }
}
