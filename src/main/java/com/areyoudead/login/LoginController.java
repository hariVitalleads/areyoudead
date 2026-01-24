package com.areyoudead.login;

import com.areyoudead.auth.AuthService;
import com.areyoudead.auth.dto.AuthResponse;
import com.areyoudead.auth.dto.LoginRequest;
import com.areyoudead.auth.dto.RegisterRequest;
import com.areyoudead.login.dto.AccountDetailsResponse;
import com.areyoudead.login.dto.ForgotPasswordRequest;
import com.areyoudead.login.dto.ForgotPasswordResponse;
import com.areyoudead.login.dto.MessageResponse;
import com.areyoudead.login.dto.ResetPasswordRequest;
import com.areyoudead.login.dto.UpdateDetailsRequest;
import com.areyoudead.security.UserPrincipal;
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
@RequestMapping("/api/login")
public class LoginController {
	private final AuthService authService;
	private final LoginService loginService;

	public LoginController(AuthService authService, LoginService loginService) {
		this.authService = authService;
		this.loginService = loginService;
	}

	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public AuthResponse signup(@Valid @RequestBody RegisterRequest req) {
		return authService.register(req);
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest req) {
		return authService.login(req);
	}

	@GetMapping("/me")
	public AccountDetailsResponse me(Authentication authentication) {
		UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
		return loginService.getAccount(principal.getUserId());
	}

	@PutMapping("/details")
	public AccountDetailsResponse updateDetails(
		Authentication authentication,
		@Valid @RequestBody UpdateDetailsRequest req
	) {
		UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
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

