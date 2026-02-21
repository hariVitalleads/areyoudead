package com.checkin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

	/** Max requests per window for login/register/forgot-password. */
	private int authRequestsPerWindow = 10;
	/** Window duration in seconds. */
	private int windowSeconds = 60;

	public int getAuthRequestsPerWindow() {
		return authRequestsPerWindow;
	}

	public void setAuthRequestsPerWindow(int authRequestsPerWindow) {
		this.authRequestsPerWindow = authRequestsPerWindow;
	}

	public int getWindowSeconds() {
		return windowSeconds;
	}

	public void setWindowSeconds(int windowSeconds) {
		this.windowSeconds = windowSeconds;
	}
}
