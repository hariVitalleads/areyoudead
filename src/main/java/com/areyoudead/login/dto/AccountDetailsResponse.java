package com.areyoudead.login.dto;

import java.time.Instant;
import java.util.UUID;

public class AccountDetailsResponse {
	private final UUID id;
	private final String email;
	private final Instant createdAt;
	private final Instant lastLoginDate;
	private final String registrationType;
	private final String firstName;
	private final String middleName;
	private final String lastName;
	private final String country;
	private final String state;
	private final String mobileNumber;
	private final String addressLine1;
	private final String addressLine2;
	private final boolean hasPaid;
	private final Instant paidAt;

	public AccountDetailsResponse(
		UUID id,
		String email,
		Instant createdAt,
		Instant lastLoginDate,
		String registrationType,
		String firstName,
		String middleName,
		String lastName,
		String country,
		String state,
		String mobileNumber,
		String addressLine1,
		String addressLine2,
		boolean hasPaid,
		Instant paidAt
	) {
		this.id = id;
		this.email = email;
		this.createdAt = createdAt;
		this.lastLoginDate = lastLoginDate;
		this.registrationType = registrationType;
		this.firstName = firstName;
		this.middleName = middleName;
		this.lastName = lastName;
		this.country = country;
		this.state = state;
		this.mobileNumber = mobileNumber;
		this.addressLine1 = addressLine1;
		this.addressLine2 = addressLine2;
		this.hasPaid = hasPaid;
		this.paidAt = paidAt;
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

	public String getRegistrationType() {
		return registrationType;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getMiddleName() {
		return middleName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getCountry() {
		return country;
	}

	public String getState() {
		return state;
	}

	public String getMobileNumber() {
		return mobileNumber;
	}

	public String getAddressLine1() {
		return addressLine1;
	}

	public String getAddressLine2() {
		return addressLine2;
	}

	public boolean isHasPaid() {
		return hasPaid;
	}

	public Instant getPaidAt() {
		return paidAt;
	}
}

