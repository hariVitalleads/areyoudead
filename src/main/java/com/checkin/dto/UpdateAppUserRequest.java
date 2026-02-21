package com.checkin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class UpdateAppUserRequest {
	@Email
	private String email;

	/** Inactivity threshold in days (1–90). Null = use global default. */
	@Min(1)
	@Max(90)
	private Integer inactivityThresholdDays;

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
}
