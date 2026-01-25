package com.areyoudead.dto;

public class AuthResponse {
	private final String tokenType = "Bearer";
	private final String accessToken;
	private final UserResponse user;

	public AuthResponse(String accessToken, UserResponse user) {
		this.accessToken = accessToken;
		this.user = user;
	}

	public String getTokenType() {
		return tokenType;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public UserResponse getUser() {
		return user;
	}
}
