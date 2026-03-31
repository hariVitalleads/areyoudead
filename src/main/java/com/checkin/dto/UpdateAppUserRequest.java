package com.checkin.dto;

import com.checkin.model.AlertChannelPreference;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateAppUserRequest {
	@Email
	private String email;

	/** Inactivity threshold in days (1–90). Null = use global default. */
	@Min(1)
	@Max(90)
	private Integer inactivityThresholdDays;

	/** How to notify emergency contacts: EMAIL, SMS, or BOTH. */
	private AlertChannelPreference alertChannelPreference;

	@Size(max = 100)
	private String firstName;

	@Size(max = 100)
	private String lastName;

	@Size(min = 6, max = 20)
	@Pattern(regexp = "^[0-9+\\-()\\s]*$", message = "mobileNumber has invalid characters")
	private String mobileNumber;

	/** Optional. FCM token. Pass to register or update the device for push notifications. Empty string clears. */
	@Size(max = 512)
	private String fcmToken;

	/** Optional. Daily reminder times in HH:mm (UTC), e.g. ["09:00", "18:00"]. Max 5 times. */
	private java.util.List<String> notificationTimes;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Integer getInactivityThresholdDays() {
		return inactivityThresholdDays;
	}

	public void setInactivityThresholdDays(Integer inactivityThresholdDays) {
		this.inactivityThresholdDays = inactivityThresholdDays;
	}

	public AlertChannelPreference getAlertChannelPreference() {
		return alertChannelPreference;
	}

	public void setAlertChannelPreference(AlertChannelPreference alertChannelPreference) {
		this.alertChannelPreference = alertChannelPreference;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getMobileNumber() {
		return mobileNumber;
	}

	public void setMobileNumber(String mobileNumber) {
		this.mobileNumber = mobileNumber;
	}

	public String getFcmToken() {
		return fcmToken;
	}

	public void setFcmToken(String fcmToken) {
		this.fcmToken = fcmToken;
	}

	public java.util.List<String> getNotificationTimes() {
		return notificationTimes;
	}

	public void setNotificationTimes(java.util.List<String> notificationTimes) {
		this.notificationTimes = notificationTimes;
	}
}
