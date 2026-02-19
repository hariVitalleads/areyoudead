package com.areyoudead.controller;

import com.areyoudead.dto.AccountDetailsResponse;
import com.areyoudead.dto.AuthResponse;
import com.areyoudead.dto.UserResponse;
import com.areyoudead.dto.ForgotPasswordRequest;
import com.areyoudead.dto.ForgotPasswordResponse;
import com.areyoudead.dto.LoginRequest;
import com.areyoudead.dto.MessageResponse;
import com.areyoudead.dto.RegisterRequest;
import com.areyoudead.dto.ResetPasswordRequest;
import com.areyoudead.dto.UpdateDetailsRequest;
import com.areyoudead.security.CurrentUser;
import com.areyoudead.security.UserPrincipal;
import com.areyoudead.service.AuthService;
import com.areyoudead.service.LoginService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/login")
public class LoginController {
	private final AuthService authService;
	private final LoginService loginService;

	public LoginController(AuthService authService, LoginService loginService) {
		this.authService = authService;
		this.loginService = loginService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public UserResponse register(@Valid @RequestBody RegisterRequest req) {
		return authService.register(req);
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest req) {
		return authService.login(req);
	}

	@GetMapping("/me")
	public AccountDetailsResponse me(@CurrentUser UserPrincipal principal) {
		return loginService.getAccount(principal.getUserId());
	}

	@PutMapping("/details")
	public AccountDetailsResponse updateDetails(
			@CurrentUser UserPrincipal principal,
			@Valid @RequestBody UpdateDetailsRequest req) {
		return loginService.updateDetails(principal.getUserId(), req);
	}

	@PostMapping("/forgot-password")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
		String token = loginService.forgotPassword(req.getEmail());
		return new ForgotPasswordResponse("If the account exists, a reset token has been issued.", token);
	}

	@PostMapping("/reset-password")
	public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
		loginService.resetPassword(req.getToken(), req.getNewPassword());
		return new MessageResponse("Password has been updated.");
	}
}
