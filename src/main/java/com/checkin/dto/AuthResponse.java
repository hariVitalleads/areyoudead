package com.checkin.dto;

public class AuthResponse {
	private final String tokenType = "Bearer";
	private final String accessToken;
	private final String refreshToken;
	private final UserResponse user;

	public AuthResponse(String accessToken, String refreshToken, UserResponse user) {
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.user = user;
	}

	/** For tests: creates response without refresh token. */
	public AuthResponse(String accessToken, UserResponse user) {
		this(accessToken, null, user);
	}

	public String getTokenType() {
		return tokenType;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public UserResponse getUser() {
		return user;
	}
}
