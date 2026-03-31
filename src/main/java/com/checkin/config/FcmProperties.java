package com.checkin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.fcm")
public class FcmProperties {
	private boolean enabled = false;
	private String credentialsPath = "";
	private String credentialsJsonBase64 = "";

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getCredentialsPath() {
		return credentialsPath;
	}

	public void setCredentialsPath(String credentialsPath) {
		this.credentialsPath = credentialsPath;
	}

	public String getCredentialsJsonBase64() {
		return credentialsJsonBase64;
	}

	public void setCredentialsJsonBase64(String credentialsJsonBase64) {
		this.credentialsJsonBase64 = credentialsJsonBase64;
	}
}
