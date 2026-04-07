package com.checkin.firebase;

import com.checkin.config.FirebaseProperties;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Verifies Firebase ID tokens using the Admin SDK. Bean is inactive when no credentials are configured.
 */
@Component
public class FirebaseIdTokenVerifier {

	private static final Logger log = LoggerFactory.getLogger(FirebaseIdTokenVerifier.class);

	private final FirebaseAuth firebaseAuth;

	public FirebaseIdTokenVerifier(FirebaseProperties properties) {
		FirebaseAuth auth = null;
		if (properties.isConfigured()) {
			try {
				if (FirebaseApp.getApps().isEmpty()) {
					GoogleCredentials credentials = loadCredentials(properties);
					FirebaseOptions options = FirebaseOptions.builder().setCredentials(credentials).build();
					FirebaseApp.initializeApp(options);
				}
				auth = FirebaseAuth.getInstance();
				log.info("Firebase Admin SDK initialized for token verification");
			} catch (Exception e) {
				log.error("Failed to initialize Firebase Admin SDK; /auth/firebase will be unavailable", e);
			}
		} else {
			log.debug("Firebase credentials not set; POST /auth/firebase will return 503");
		}
		this.firebaseAuth = auth;
	}

	private static GoogleCredentials loadCredentials(FirebaseProperties properties) throws java.io.IOException {
		if (properties.getCredentialsJsonBase64() != null && !properties.getCredentialsJsonBase64().isBlank()) {
			byte[] json = Base64.getDecoder().decode(properties.getCredentialsJsonBase64().trim());
			return GoogleCredentials.fromStream(new ByteArrayInputStream(json));
		}
		Path path = Path.of(properties.getCredentialsPath().trim());
		try (var in = Files.newInputStream(path)) {
			return GoogleCredentials.fromStream(in);
		}
	}

	public boolean isConfigured() {
		return firebaseAuth != null;
	}

	public FirebaseIdentity verify(String idToken) throws FirebaseAuthException {
		if (firebaseAuth == null) {
			throw new IllegalStateException("Firebase is not configured");
		}
		FirebaseToken token = firebaseAuth.verifyIdToken(idToken);
		String email = token.getEmail();
		boolean emailVerified = token.isEmailVerified();
		String name = token.getName();
		return new FirebaseIdentity(token.getUid(), email, emailVerified, name);
	}
}
