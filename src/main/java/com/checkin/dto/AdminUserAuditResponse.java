package com.checkin.dto;

import com.checkin.model.AlertChannelPreference;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full audit view for a user: details, audit events, and emergency contacts.
 */
public class AdminUserAuditResponse {
	private final UUID id;
	private final String email;
	private final Instant createdAt;
	private final Instant lastLoginDate;
	private final Instant lastManualCheckInAt;
	private final Instant firstAlertSentAt;
	private final Integer contactsAlertedCount;
	private final Integer inactivityThresholdDays;
	private final AlertChannelPreference alertChannelPreference;
	private final List<AuditEventResponse> auditEvents;
	private final List<EmergencyContactResponse> emergencyContacts;

	public AdminUserAuditResponse(UUID id, String email, Instant createdAt, Instant lastLoginDate,
			Instant lastManualCheckInAt, Instant firstAlertSentAt, Integer contactsAlertedCount,
			Integer inactivityThresholdDays, AlertChannelPreference alertChannelPreference,
			List<AuditEventResponse> auditEvents, List<EmergencyContactResponse> emergencyContacts) {
		this.id = id;
		this.email = email;
		this.createdAt = createdAt;
		this.lastLoginDate = lastLoginDate;
		this.lastManualCheckInAt = lastManualCheckInAt;
		this.firstAlertSentAt = firstAlertSentAt;
		this.contactsAlertedCount = contactsAlertedCount;
		this.inactivityThresholdDays = inactivityThresholdDays;
		this.alertChannelPreference = alertChannelPreference;
		this.auditEvents = auditEvents != null ? List.copyOf(auditEvents) : List.of();
		this.emergencyContacts = emergencyContacts != null ? List.copyOf(emergencyContacts) : List.of();
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

	public Instant getLastManualCheckInAt() {
		return lastManualCheckInAt;
	}

	public Instant getFirstAlertSentAt() {
		return firstAlertSentAt;
	}

	public Integer getContactsAlertedCount() {
		return contactsAlertedCount;
	}

	public Integer getInactivityThresholdDays() {
		return inactivityThresholdDays;
	}

	public AlertChannelPreference getAlertChannelPreference() {
		return alertChannelPreference;
	}

	public List<AuditEventResponse> getAuditEvents() {
		return auditEvents;
	}

	public List<EmergencyContactResponse> getEmergencyContacts() {
		return emergencyContacts;
	}
}
