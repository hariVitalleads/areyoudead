package com.checkin.dto;

import jakarta.validation.constraints.Email;

public class UpdateAppUserRequest {
	@Email
	private String email;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
}
