package com.areyoudead.login.dto;

public class ForgotPasswordResponse {
	private final String message;
	private final String resetToken;

	public ForgotPasswordResponse(String message, String resetToken) {
		this.message = message;
		this.resetToken = resetToken;
	}

	public String getMessage() {
		return message;
	}

	public String getResetToken() {
		return resetToken;
	}
}

