package com.areyoudead.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the emergency-contact email notification
 * channel.
 * Mirrors {@link EmergencyContactProperties} (the SMS counterpart).
 *
 * <p>
 * Bound from the {@code app.emergency-contacts.email} prefix. All values are
 * overridable via environment variables — see {@code application.yml} for the
 * full list of supported env vars.
 */
@Validated
@ConfigurationProperties(prefix = "app.emergency-contacts.email")
public class EmailProperties {

    /** Whether the email notification channel is active. */
    private boolean enabled = true;

    /** The {@code From:} address used on every alert email. */
    private String fromAddress = "noreply@areyoudead.com";

    /** Prefix prepended to every alert email subject, e.g. "[AreYouAlive]". */
    private String subjectPrefix = "[AreYouAlive]";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getSubjectPrefix() {
        return subjectPrefix;
    }

    public void setSubjectPrefix(String subjectPrefix) {
        this.subjectPrefix = subjectPrefix;
    }
}
