package com.checkin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.firebase")
public class FirebaseProperties {

	/** Base64-encoded Firebase service account JSON (server-only). */
	private String credentialsJsonBase64 = "";

	/** Path to service account JSON file (alternative to base64). */
	private String credentialsPath = "";

	public boolean isConfigured() {
		return (credentialsJsonBase64 != null && !credentialsJsonBase64.isBlank())
				|| (credentialsPath != null && !credentialsPath.isBlank());
	}

	public String getCredentialsJsonBase64() {
		return credentialsJsonBase64;
	}

	public void setCredentialsJsonBase64(String credentialsJsonBase64) {
		this.credentialsJsonBase64 = credentialsJsonBase64;
	}

	public String getCredentialsPath() {
		return credentialsPath;
	}

	public void setCredentialsPath(String credentialsPath) {
		this.credentialsPath = credentialsPath;
	}
}
