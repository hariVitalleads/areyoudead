package com.checkin.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {
	@NotBlank
	private String secret;

	@NotBlank
	private String issuer;

	@Min(60)
	private long accessTokenTtlSeconds = 3600;

	@Min(60)
	private long refreshTokenTtlSeconds = 86400; // 1 day (24h)

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public long getAccessTokenTtlSeconds() {
		return accessTokenTtlSeconds;
	}

	public void setAccessTokenTtlSeconds(long accessTokenTtlSeconds) {
		this.accessTokenTtlSeconds = accessTokenTtlSeconds;
	}

	public long getRefreshTokenTtlSeconds() {
		return refreshTokenTtlSeconds;
	}

	public void setRefreshTokenTtlSeconds(long refreshTokenTtlSeconds) {
		this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
	}
}

