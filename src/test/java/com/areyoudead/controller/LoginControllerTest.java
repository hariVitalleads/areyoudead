package com.areyoudead.controller;

import com.areyoudead.dto.AccountDetailsResponse;
import com.areyoudead.dto.AuthResponse;
import com.areyoudead.dto.ForgotPasswordRequest;
import com.areyoudead.dto.ForgotPasswordResponse;
import com.areyoudead.dto.LoginRequest;
import com.areyoudead.dto.MessageResponse;
import com.areyoudead.dto.RegisterRequest;
import com.areyoudead.dto.ResetPasswordRequest;
import com.areyoudead.dto.UpdateDetailsRequest;
import com.areyoudead.dto.UserResponse;
import com.areyoudead.security.JwtService;
import com.areyoudead.security.UserPrincipal;
import com.areyoudead.service.AuthService;
import com.areyoudead.service.LoginService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoginController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private LoginService loginService;

    @MockitoBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID userId;
    private UserResponse userResponse;
    private AuthResponse authResponse;
    private AccountDetailsResponse accountDetailsResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userResponse = new UserResponse(userId, "test@example.com", Instant.now());
        authResponse = new AuthResponse("jwt-token", userResponse);
        accountDetailsResponse = new AccountDetailsResponse(
                userId, "test@example.com", Instant.now(), Instant.now(),
                "STANDARD", "John", null, "Doe",
                "USA", "CA", "+1234567890",
                "123 Main St", null, false, null);
    }

    private Authentication auth() {
        return new UsernamePasswordAuthenticationToken(
                new UserPrincipal(userId, "test@example.com"), null, null);
    }

    @Test
    void register_Success() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(authService.register(any(RegisterRequest.class))).thenReturn(userResponse);

        // When & Then
        mockMvc.perform(post("/api/login/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void login_Success() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        // When & Then
        mockMvc.perform(post("/api/login/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.user.id").value(userId.toString()));
    }

    @Test
    void me_Success() throws Exception {
        // Given
        when(loginService.getAccount(eq(userId))).thenReturn(accountDetailsResponse);

        // When & Then
        mockMvc.perform(get("/api/login/me").with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    void updateDetails_Success() throws Exception {
        // Given
        UpdateDetailsRequest request = new UpdateDetailsRequest();
        request.setFirstName("Jane");
        request.setLastName("Smith");

        when(loginService.updateDetails(eq(userId), any(UpdateDetailsRequest.class)))
                .thenReturn(accountDetailsResponse);

        // When & Then
        mockMvc.perform(put("/api/login/details")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(auth()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()));
    }

    @Test
    void forgotPassword_Success() throws Exception {
        // Given
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@example.com");
        String token = "reset-token";

        when(loginService.forgotPassword(anyString())).thenReturn(token);

        // When & Then
        mockMvc.perform(post("/api/login/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("If the account exists, a reset token has been issued."))
                .andExpect(jsonPath("$.resetToken").value(token));
    }

    @Test
    void resetPassword_Success() throws Exception {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("reset-token");
        request.setNewPassword("newPassword123");

        // When & Then
        mockMvc.perform(post("/api/login/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password has been updated."));
    }

    @Test
    void register_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        // Missing required fields

        // When & Then
        mockMvc.perform(post("/api/login/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }
}
