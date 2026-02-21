package com.checkin.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.user")
public class UserVerificationProperties {

	/** When true, users must verify email before they can log in. */
	private boolean requireEmailVerification = true;

	/** Verification token validity in seconds (default 24 hours). */
	@Min(60)
	private int verificationTokenTtlSeconds = 86400;

	public boolean isRequireEmailVerification() {
		return requireEmailVerification;
	}

	public void setRequireEmailVerification(boolean requireEmailVerification) {
		this.requireEmailVerification = requireEmailVerification;
	}

	public int getVerificationTokenTtlSeconds() {
		return verificationTokenTtlSeconds;
	}

	public void setVerificationTokenTtlSeconds(int verificationTokenTtlSeconds) {
		this.verificationTokenTtlSeconds = verificationTokenTtlSeconds;
	}
}
