package com.areyoudead.security;

import com.areyoudead.config.JwtProperties;
import com.areyoudead.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
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

	public Jws<Claims> parseAndValidate(String token) {
		return Jwts.parser()
			.verifyWith(key)
			.build()
			.parseSignedClaims(token);
	}
}

