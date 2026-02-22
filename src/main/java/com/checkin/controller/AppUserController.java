package com.checkin.controller;

import com.checkin.dto.AppUserDetailsResponse;
import com.checkin.dto.AuditEventResponse;
import com.checkin.dto.AuthResponse;
import com.checkin.dto.ForgotPasswordRequest;
import com.checkin.dto.ForgotPasswordResponse;
import com.checkin.dto.LoginRequest;
import com.checkin.dto.MessageResponse;
import com.checkin.dto.RefreshTokenRequest;
import com.checkin.dto.RegisterRequest;
import com.checkin.dto.ResetPasswordRequest;
import com.checkin.dto.UpdateAppUserRequest;
import com.checkin.dto.UserResponse;
import com.checkin.security.CurrentUser;
import com.checkin.security.UserPrincipal;
import com.checkin.service.AppUserService;
import com.checkin.service.AuditService;
import com.checkin.service.AuthService;
import com.checkin.service.LoginService;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class AppUserController {
	private final AuthService authService;
	private final LoginService loginService;
	private final AppUserService appUserService;
	private final AuditService auditService;

	public AppUserController(AuthService authService, LoginService loginService, AppUserService appUserService,
			AuditService auditService) {
		this.authService = authService;
		this.loginService = loginService;
		this.appUserService = appUserService;
		this.auditService = auditService;
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

	@PostMapping("/refresh")
	public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest req) {
		return authService.refresh(req.getRefreshToken());
	}

	/**
	 * Public endpoint: verify user email. Called when user clicks verification link from registration email.
	 */
	@GetMapping("/verify-email/{token}")
	@ResponseStatus(HttpStatus.OK)
	public MessageResponse verifyEmail(@PathVariable String token) {
		authService.verifyUserByToken(token);
		return new MessageResponse("Your email has been verified. You can now log in.");
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

	@GetMapping("/me")
	public AppUserDetailsResponse me(@CurrentUser UserPrincipal principal) {
		return appUserService.me(principal.getUserId());
	}

	@GetMapping("/audit-events")
	public List<AuditEventResponse> getAuditEvents(@CurrentUser UserPrincipal principal) {
		return auditService.listForUser(principal.getUserId());
	}

	@PostMapping("/check-in")
	public MessageResponse checkIn(@CurrentUser UserPrincipal principal,
			@RequestBody(required = false) @Valid com.checkin.dto.CheckInRequest req) {
		Integer snoozeDays = (req != null && req.getSnoozeDays() != null) ? req.getSnoozeDays() : null;
		appUserService.checkIn(principal.getUserId(), snoozeDays);
		if (snoozeDays != null && snoozeDays > 0) {
			return new MessageResponse("Check-in recorded. Alerts snoozed for " + snoozeDays + " days.");
		}
		return new MessageResponse("Check-in recorded. You will not receive inactivity alerts until your next period of inactivity.");
	}

	@PutMapping("/details")
	public AppUserDetailsResponse updateDetails(
			@CurrentUser UserPrincipal principal,
			@Valid @RequestBody UpdateAppUserRequest req) {
		return appUserService.update(principal.getUserId(), req);
	}
}
