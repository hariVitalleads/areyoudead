package com.checkin.dto;

import java.time.Instant;
import java.util.UUID;

public class AuditEventResponse {
	private final UUID id;
	private final String action;
	private final String details;
	private final Instant createdAt;

	public AuditEventResponse(UUID id, String action, String details, Instant createdAt) {
		this.id = id;
		this.action = action;
		this.details = details;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public String getAction() {
		return action;
	}

	public String getDetails() {
		return details;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
