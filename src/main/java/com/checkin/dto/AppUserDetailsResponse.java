package com.checkin.dto;

import java.time.Instant;
import java.util.UUID;

public class AppUserDetailsResponse {
	private final UUID id;
	private final String email;
	private final Instant createdAt;
	private final Instant lastLoginDate;

	public AppUserDetailsResponse(UUID id, String email, Instant createdAt, Instant lastLoginDate) {
		this.id = id;
		this.email = email;
		this.createdAt = createdAt;
		this.lastLoginDate = lastLoginDate;
	}

	public UUID getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getLastLoginDate() {
		return lastLoginDate;
	}
}
