package com.checkin.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class User {
	@Id
	private UUID id;

	@Column(nullable = false)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "last_login_date")
	private Instant lastLoginDate;

	@Column(name = "password_reset_token_hash")
	private String passwordResetTokenHash;

	@Column(name = "password_reset_expires_at")
	private Instant passwordResetExpiresAt;

	@Column(name = "last_manual_check_in_at")
	private Instant lastManualCheckInAt;

	@Column(name = "alerts_snoozed_until")
	private Instant alertsSnoozedUntil;

	@Column(name = "email_verification_token")
	private String emailVerificationToken;

	@Column(name = "email_verification_token_expires_at")
	private Instant emailVerificationTokenExpiresAt;

	@Column(name = "email_verified_at")
	private Instant emailVerifiedAt;

	@Column(name = "inactivity_threshold_days")
	private Short inactivityThresholdDays;

	@Column(name = "first_alert_sent_at")
	private Instant firstAlertSentAt;

	@Column(name = "contacts_alerted_count")
	private Short contactsAlertedCount;

	@Enumerated(EnumType.STRING)
	@Column(name = "alert_channel_preference")
	private AlertChannelPreference alertChannelPreference;

	@Column(name = "super_user", nullable = false)
	private boolean superUser = false;

	protected User() {}

	public User(UUID id, String email, String passwordHash, Instant createdAt) {
		this.id = id;
		this.email = email;
		this.passwordHash = passwordHash;
		this.createdAt = createdAt;
		this.lastLoginDate = null;
		this.passwordResetTokenHash = null;
		this.passwordResetExpiresAt = null;
	}

	public UUID getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getLastLoginDate() {
		return lastLoginDate;
	}

	public void setLastLoginDate(Instant lastLoginDate) {
		this.lastLoginDate = lastLoginDate;
	}

	public String getPasswordResetTokenHash() {
		return passwordResetTokenHash;
	}

	public void setPasswordResetTokenHash(String passwordResetTokenHash) {
		this.passwordResetTokenHash = passwordResetTokenHash;
	}

	public Instant getPasswordResetExpiresAt() {
		return passwordResetExpiresAt;
	}

	public void setPasswordResetExpiresAt(Instant passwordResetExpiresAt) {
		this.passwordResetExpiresAt = passwordResetExpiresAt;
	}

	public Instant getLastManualCheckInAt() {
		return lastManualCheckInAt;
	}

	public void setLastManualCheckInAt(Instant lastManualCheckInAt) {
		this.lastManualCheckInAt = lastManualCheckInAt;
	}

	public Instant getAlertsSnoozedUntil() {
		return alertsSnoozedUntil;
	}

	public void setAlertsSnoozedUntil(Instant alertsSnoozedUntil) {
		this.alertsSnoozedUntil = alertsSnoozedUntil;
	}

	public String getEmailVerificationToken() {
		return emailVerificationToken;
	}

	public void setEmailVerificationToken(String emailVerificationToken) {
		this.emailVerificationToken = emailVerificationToken;
	}

	public Instant getEmailVerificationTokenExpiresAt() {
		return emailVerificationTokenExpiresAt;
	}

	public void setEmailVerificationTokenExpiresAt(Instant emailVerificationTokenExpiresAt) {
		this.emailVerificationTokenExpiresAt = emailVerificationTokenExpiresAt;
	}

	public Instant getEmailVerifiedAt() {
		return emailVerifiedAt;
	}

	public void setEmailVerifiedAt(Instant emailVerifiedAt) {
		this.emailVerifiedAt = emailVerifiedAt;
	}

	public Short getInactivityThresholdDays() {
		return inactivityThresholdDays;
	}

	public void setInactivityThresholdDays(Short inactivityThresholdDays) {
		this.inactivityThresholdDays = inactivityThresholdDays;
	}

	public Instant getFirstAlertSentAt() {
		return firstAlertSentAt;
	}

	public void setFirstAlertSentAt(Instant firstAlertSentAt) {
		this.firstAlertSentAt = firstAlertSentAt;
	}

	public Short getContactsAlertedCount() {
		return contactsAlertedCount;
	}

	public void setContactsAlertedCount(Short contactsAlertedCount) {
		this.contactsAlertedCount = contactsAlertedCount;
	}

	public AlertChannelPreference getAlertChannelPreference() {
		return alertChannelPreference;
	}

	public void setAlertChannelPreference(AlertChannelPreference alertChannelPreference) {
		this.alertChannelPreference = alertChannelPreference;
	}

	public boolean isSuperUser() {
		return superUser;
	}

	public void setSuperUser(boolean superUser) {
		this.superUser = superUser;
	}
}

