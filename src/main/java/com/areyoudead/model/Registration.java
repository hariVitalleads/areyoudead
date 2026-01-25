package com.areyoudead.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "registration")
public class Registration {
	@Id
	@Column(name = "user_id")
	private UUID userId;

	@Column(name = "registration_type", nullable = false)
	private String registrationType;

	@Column(name = "first_name")
	private String firstName;

	@Column(name = "middle_name")
	private String middleName;

	@Column(name = "last_name")
	private String lastName;

	@Column(name = "country")
	private String country;

	@Column(name = "state")
	private String state;

	@Column(name = "mobile_number")
	private String mobileNumber;

	@Column(name = "address_line_1")
	private String addressLine1;

	@Column(name = "address_line_2")
	private String addressLine2;

	@Column(name = "has_paid", nullable = false)
	private boolean hasPaid;

	@Column(name = "paid_at")
	private Instant paidAt;

	protected Registration() {}

	public Registration(UUID userId, String registrationType, boolean hasPaid, Instant paidAt) {
		this.userId = userId;
		this.registrationType = registrationType;
		this.firstName = null;
		this.middleName = null;
		this.lastName = null;
		this.country = null;
		this.state = null;
		this.mobileNumber = null;
		this.addressLine1 = null;
		this.addressLine2 = null;
		this.hasPaid = hasPaid;
		this.paidAt = paidAt;
	}

	public UUID getUserId() {
		return userId;
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

	public boolean isHasPaid() {
		return hasPaid;
	}

	public void setHasPaid(boolean hasPaid) {
		this.hasPaid = hasPaid;
	}

	public Instant getPaidAt() {
		return paidAt;
	}

	public void setPaidAt(Instant paidAt) {
		this.paidAt = paidAt;
	}
}

