package com.areyoudead.user;

import com.areyoudead.auth.AuthService;
import com.areyoudead.auth.dto.AuthResponse;
import com.areyoudead.auth.dto.RegisterRequest;
import com.areyoudead.login.LoginService;
import com.areyoudead.login.dto.ForgotPasswordRequest;
import com.areyoudead.login.dto.ForgotPasswordResponse;
import com.areyoudead.login.dto.MessageResponse;
import com.areyoudead.login.dto.ResetPasswordRequest;
import com.areyoudead.security.UserPrincipal;
import com.areyoudead.user.dto.AppUserDetailsResponse;
import com.areyoudead.user.dto.UpdateAppUserRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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

	public AppUserController(AuthService authService, LoginService loginService, AppUserService appUserService) {
		this.authService = authService;
		this.loginService = loginService;
		this.appUserService = appUserService;
	}

	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public AuthResponse signup(@Valid @RequestBody RegisterRequest req) {
		return authService.register(req);
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
	public AppUserDetailsResponse me(Authentication authentication) {
		UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
		return appUserService.me(principal.getUserId());
	}

	@PutMapping("/details")
	public AppUserDetailsResponse updateDetails(
		Authentication authentication,
		@Valid @RequestBody UpdateAppUserRequest req
	) {
		UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
		return appUserService.update(principal.getUserId(), req.getEmail());
	}
}

