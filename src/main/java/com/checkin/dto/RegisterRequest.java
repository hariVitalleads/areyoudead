package com.checkin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
	@Email
	@NotBlank
	private String email;

	@NotBlank
	@Size(min = 8, max = 200)
	private String password;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

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
	private String mobileNumber;

	@Size(min = 1, max = 200)
	private String addressLine1;

	@Size(min = 1, max = 200)
	private String addressLine2;

	/** Optional. FCM token for push notifications. Can also be set later via PUT /details. */
	@Size(max = 512)
	private String fcmToken;

	/** Optional. Daily reminder times in HH:mm (UTC), e.g. ["09:00", "18:00"]. */
	private java.util.List<String> notificationTimes;

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
