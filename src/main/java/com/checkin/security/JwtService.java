package com.checkin.security;

import com.checkin.config.JwtProperties;
import com.checkin.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
	private final JwtProperties props;
	private final SecretKey key;

	public JwtService(JwtProperties props) {
		this.props = props;
		byte[] secretBytes = props.getSecret().getBytes(StandardCharsets.UTF_8);
		if (secretBytes.length < 32) {
			throw new IllegalArgumentException(
				"security.jwt.secret must be at least 32 bytes for HS256 (got " + secretBytes.length + ")"
			);
		}
		this.key = Keys.hmacShaKeyFor(secretBytes);
	}

	public String createAccessToken(User user) {
		Instant now = Instant.now();
		Instant exp = now.plusSeconds(props.getAccessTokenTtlSeconds());

		return Jwts.builder()
			.issuer(props.getIssuer())
			.subject(user.getId().toString())
			.claim("email", user.getEmail())
			.issuedAt(Date.from(now))
			.expiration(Date.from(exp))
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();
	}

	/** Creates a refresh token string (opaque, to be hashed before storage). */
	public String createRefreshTokenValue() {
		return UUID.randomUUID().toString() + "-" + System.nanoTime();
	}

	public long getRefreshTokenTtlSeconds() {
		return props.getRefreshTokenTtlSeconds();
	}

	public static String hashToken(String token) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] hash = md.digest(token.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}

	public Jws<Claims> parseAndValidate(String token) {
		return Jwts.parser()
			.verifyWith(key)
			.build()
			.parseSignedClaims(token);
	}
}

