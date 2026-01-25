package com.areyoudead.dto;

import java.time.Instant;
import java.util.UUID;

public class UserResponse {
	private UUID id;
	private String email;
	private Instant createdAt;

	public UserResponse(UUID id, String email, Instant createdAt) {
		this.id = id;
		this.email = email;
		this.createdAt = createdAt;
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
}
