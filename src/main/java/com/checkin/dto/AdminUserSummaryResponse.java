package com.checkin.dto;

import com.checkin.model.AlertChannelPreference;
import java.time.Instant;
import java.util.UUID;

/**
 * Summary of a user for admin audit list. Excludes sensitive data.
 */
public class AdminUserSummaryResponse {
	private final UUID id;
	private final String email;
	private final Instant createdAt;
	private final Instant lastLoginDate;
	private final Instant lastManualCheckInAt;
	private final Integer inactivityThresholdDays;
	private final AlertChannelPreference alertChannelPreference;

	public AdminUserSummaryResponse(UUID id, String email, Instant createdAt, Instant lastLoginDate,
			Instant lastManualCheckInAt, Integer inactivityThresholdDays, AlertChannelPreference alertChannelPreference) {
		this.id = id;
		this.email = email;
		this.createdAt = createdAt;
		this.lastLoginDate = lastLoginDate;
		this.lastManualCheckInAt = lastManualCheckInAt;
		this.inactivityThresholdDays = inactivityThresholdDays;
		this.alertChannelPreference = alertChannelPreference;
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

	public Integer getInactivityThresholdDays() {
		return inactivityThresholdDays;
	}

	public AlertChannelPreference getAlertChannelPreference() {
		return alertChannelPreference;
	}
}
