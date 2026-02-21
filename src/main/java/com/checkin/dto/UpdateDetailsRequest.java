package com.checkin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateDetailsRequest {
	@Email
	private String email;

	@Size(min = 1, max = 50)
	private String registrationType;

	@Size(min = 1, max = 100)
	private String firstName;

	@Size(min = 1, max = 100)
	private String middleName;

	@Size(min = 1, max = 100)
	private String lastName;

	@Size(min = 1, max = 100)
	private String country;

	@Size(min = 1, max = 100)
	private String state;

	@Size(min = 6, max = 20)
	@Pattern(regexp = "^[0-9+\\-()\\s]+$", message = "mobileNumber has invalid characters")
	private String mobileNumber;

	@Size(min = 1, max = 200)
	private String addressLine1;

	@Size(min = 1, max = 200)
	private String addressLine2;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getRegistrationType() {
		return registrationType;
	}

	public void setRegistrationType(String registrationType) {
		this.registrationType = registrationType;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getMiddleName() {
		return middleName;
	}

	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getMobileNumber() {
		return mobileNumber;
	}

	public void setMobileNumber(String mobileNumber) {
		this.mobileNumber = mobileNumber;
	}

	public String getAddressLine1() {
		return addressLine1;
	}

	public void setAddressLine1(String addressLine1) {
		this.addressLine1 = addressLine1;
	}

	public String getAddressLine2() {
		return addressLine2;
	}

	public void setAddressLine2(String addressLine2) {
		this.addressLine2 = addressLine2;
	}
}
