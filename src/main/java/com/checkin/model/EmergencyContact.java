package com.checkin.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "emergency_contact")
public class EmergencyContact {
	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "contact_index", nullable = false)
	private short contactIndex;

	@Column(name = "mobile_number", nullable = false)
	private String mobileNumber;

	@Column(nullable = false)
	private String email;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected EmergencyContact() {}

	public EmergencyContact(
		UUID id,
		UUID userId,
		short contactIndex,
		String mobileNumber,
		String email,
		Instant createdAt
	) {
		this.id = id;
		this.userId = userId;
		this.contactIndex = contactIndex;
		this.mobileNumber = mobileNumber;
		this.email = email;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public short getContactIndex() {
		return contactIndex;
	}

	public void setContactIndex(short contactIndex) {
		this.contactIndex = contactIndex;
	}

	public String getMobileNumber() {
		return mobileNumber;
	}

	public void setMobileNumber(String mobileNumber) {
		this.mobileNumber = mobileNumber;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}

