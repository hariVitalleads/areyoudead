package com.checkin.security;

import java.security.Principal;
import java.util.UUID;

public class UserPrincipal implements Principal {
	private final UUID userId;
	private final String email;

	public UserPrincipal(UUID userId, String email) {
		this.userId = userId;
		this.email = email;
	}

	public UUID getUserId() {
		return userId;
	}

	public String getEmail() {
		return email;
	}

	@Override
	public String getName() {
		return email;
	}
}

