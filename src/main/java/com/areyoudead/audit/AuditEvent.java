package com.areyoudead.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_event")
public class AuditEvent {
	@Id
	private UUID id;

	@Column(name = "user_id")
	private UUID userId;

	@Column(nullable = false)
	private String action;

	@Column
	private String details;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected AuditEvent() {}

	public AuditEvent(UUID id, UUID userId, String action, String details, Instant createdAt) {
		this.id = id;
		this.userId = userId;
		this.action = action;
		this.details = details;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
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

