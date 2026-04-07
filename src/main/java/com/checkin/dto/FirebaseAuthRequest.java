package com.checkin.dto;

import jakarta.validation.constraints.NotBlank;

/** Body for POST /api/user/auth/firebase — ID token from the Firebase Web SDK after sign-in. */
public class FirebaseAuthRequest {
	@NotBlank
	private String idToken;

	public String getIdToken() {
		return idToken;
	}

	public void setIdToken(String idToken) {
		this.idToken = idToken;
	}
}
