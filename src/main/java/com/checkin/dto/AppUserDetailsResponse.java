package com.checkin.dto;

import com.checkin.model.AlertChannelPreference;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class AppUserDetailsResponse {
	private final UUID id;
	private final String email;
	private final Instant createdAt;
	private final Instant lastLoginDate;
	private final Integer inactivityThresholdDays;
	private final AlertChannelPreference alertChannelPreference;
	private final String firstName;
	private final String lastName;
	private final String mobileNumber;
	private final String fcmToken;
	private final List<String> notificationTimes;

	public AppUserDetailsResponse(UUID id, String email, Instant createdAt, Instant lastLoginDate) {
		this(id, email, createdAt, lastLoginDate, null, null, null, null, null, null, null);
	}

	public AppUserDetailsResponse(UUID id, String email, Instant createdAt, Instant lastLoginDate,
			Integer inactivityThresholdDays) {
		this(id, email, createdAt, lastLoginDate, inactivityThresholdDays, null, null, null, null, null, null);
	}

	public AppUserDetailsResponse(UUID id, String email, Instant createdAt, Instant lastLoginDate,
			Integer inactivityThresholdDays, AlertChannelPreference alertChannelPreference,
			String firstName, String lastName, String mobileNumber) {
		this(id, email, createdAt, lastLoginDate, inactivityThresholdDays, alertChannelPreference, firstName, lastName, mobileNumber, null, null);
	}

	public AppUserDetailsResponse(UUID id, String email, Instant createdAt, Instant lastLoginDate,
			Integer inactivityThresholdDays, AlertChannelPreference alertChannelPreference,
			String firstName, String lastName, String mobileNumber, String fcmToken, List<String> notificationTimes) {
		this.id = id;
		this.email = email;
		this.createdAt = createdAt;
		this.lastLoginDate = lastLoginDate;
		this.inactivityThresholdDays = inactivityThresholdDays;
		this.alertChannelPreference = alertChannelPreference;
		this.firstName = firstName;
		this.lastName = lastName;
		this.mobileNumber = mobileNumber;
		this.fcmToken = fcmToken;
		this.notificationTimes = notificationTimes != null ? List.copyOf(notificationTimes) : null;
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

	public Integer getInactivityThresholdDays() {
		return inactivityThresholdDays;
	}

	public AlertChannelPreference getAlertChannelPreference() {
		return alertChannelPreference;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getMobileNumber() {
		return mobileNumber;
	}

	public String getFcmToken() {
		return fcmToken;
	}

	public List<String> getNotificationTimes() {
		return notificationTimes;
	}
}
