package com.checkin.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for the maximum number of emergency contacts per user.
 * Validated against the DB constraint (contact_index 1..20).
 */
@Validated
@ConfigurationProperties(prefix = "app.emergency-contacts")
public class EmergencyContactLimitProperties {

    /** Maximum number of emergency contacts allowed per user (1–20). */
    @Min(1)
    @Max(20)
    private int maxCount = 3;

    /** When true, only verified contacts receive inactivity alerts. */
    private boolean requireVerification = true;

    /** Verification token validity in seconds (default 24 hours). */
    @Min(60)
    private int verificationTokenTtlSeconds = 86400;

    public int getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    public boolean isRequireVerification() {
        return requireVerification;
    }

    public void setRequireVerification(boolean requireVerification) {
        this.requireVerification = requireVerification;
    }

    public int getVerificationTokenTtlSeconds() {
        return verificationTokenTtlSeconds;
    }

    public void setVerificationTokenTtlSeconds(int verificationTokenTtlSeconds) {
        this.verificationTokenTtlSeconds = verificationTokenTtlSeconds;
    }
}
