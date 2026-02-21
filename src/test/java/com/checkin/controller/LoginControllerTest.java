package com.checkin.controller;

import com.checkin.dto.AuthResponse;
import com.checkin.dto.LoginRequest;
import com.checkin.dto.UserResponse;
import com.checkin.security.JwtService;
import com.checkin.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
	private JwtService jwtService;

	@Autowired
	private ObjectMapper objectMapper;

	private UUID userId;
	private UserResponse userResponse;
	private AuthResponse authResponse;

	@BeforeEach
	void setUp() {
		userId = UUID.randomUUID();
		userResponse = new UserResponse(userId, "test@example.com", Instant.now());
		authResponse = new AuthResponse("jwt-token", userResponse);
	}

	@Test
	void login_Success() throws Exception {
		LoginRequest request = new LoginRequest();
		request.setEmail("test@example.com");
		request.setPassword("password123");

		when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

		mockMvc.perform(post("/api/login/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("jwt-token"))
				.andExpect(jsonPath("$.user.id").value(userId.toString()));
	}

	@Test
	void login_InvalidRequest_ReturnsBadRequest() throws Exception {
		LoginRequest request = new LoginRequest();

		mockMvc.perform(post("/api/login/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
						.with(csrf()))
				.andExpect(status().isBadRequest());
	}
}
