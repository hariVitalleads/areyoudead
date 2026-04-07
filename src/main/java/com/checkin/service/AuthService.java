package com.checkin.service;

import com.checkin.audit.AuditAction;
import com.checkin.config.AppMetrics;
import com.checkin.config.EmailProperties;
import com.checkin.config.UserVerificationProperties;
import com.checkin.dto.AuthResponse;
import com.checkin.dto.FirebaseAuthRequest;
import com.checkin.dto.LoginRequest;
import com.checkin.dto.RegisterRequest;
import com.checkin.dto.UserResponse;
import com.checkin.firebase.FirebaseIdentity;
import com.checkin.firebase.FirebaseIdTokenVerifier;
import com.checkin.model.AuthProvider;
import com.checkin.model.RefreshToken;
import com.checkin.model.Registration;
import com.checkin.model.User;
import com.checkin.repository.RefreshTokenRepository;
import com.checkin.repository.RegistrationRepository;
import com.checkin.repository.UserRepository;
import com.checkin.security.JwtService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import com.google.firebase.auth.FirebaseAuthException;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	private final UserRepository userRepository;
	private final RegistrationRepository registrationRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuditService auditService;
	private final AppMetrics metrics;
	private final EmailProperties emailProperties;
	private final UserVerificationProperties userVerificationProperties;
	private final JavaMailSender mailSender;
	private final UserVerificationTemplate userVerificationTemplate;
	private final FirebaseIdTokenVerifier firebaseIdTokenVerifier;

	public AuthService(
			UserRepository userRepository,
			RegistrationRepository registrationRepository,
			RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder,
			JwtService jwtService,
			AuditService auditService,
			AppMetrics metrics,
			EmailProperties emailProperties,
			UserVerificationProperties userVerificationProperties,
			JavaMailSender mailSender,
			UserVerificationTemplate userVerificationTemplate,
			FirebaseIdTokenVerifier firebaseIdTokenVerifier) {
		this.userRepository = userRepository;
		this.registrationRepository = registrationRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.auditService = auditService;
		this.metrics = metrics;
		this.emailProperties = emailProperties;
		this.userVerificationProperties = userVerificationProperties;
		this.mailSender = mailSender;
		this.userVerificationTemplate = userVerificationTemplate;
		this.firebaseIdTokenVerifier = firebaseIdTokenVerifier;
	}

	public UserResponse register(RegisterRequest req) {
		String email = normalizeEmail(req.getEmail());
		log.info("Registration attempt for email={}", email);
		if (userRepository.existsByEmail(email)) {
			log.info("Registration rejected (already registered) email={}", email);
			throw new ResponseStatusException(HttpStatus.CONFLICT, "email already registered");
		}

		User user = new User(
				UUID.randomUUID(),
				email,
				passwordEncoder.encode(req.getPassword()),
				Instant.now());
		user.setAuthProvider(AuthProvider.LOCAL);

		if (!userVerificationProperties.isRequireEmailVerification()) {
			user.setEmailVerifiedAt(Instant.now());
		} else {
			String token = UUID.randomUUID().toString();
			user.setEmailVerificationToken(token);
			user.setEmailVerificationTokenExpiresAt(
					Instant.now().plusSeconds(userVerificationProperties.getVerificationTokenTtlSeconds()));
		}

		try {
			User saved = userRepository.save(user);
			Registration reg = new Registration(
					saved.getId(),
					req.getRegistrationType() != null ? req.getRegistrationType() : "STANDARD",
					false,
					null);
			reg.setFirstName(req.getFirstName());
			reg.setMiddleName(req.getMiddleName());
			reg.setLastName(req.getLastName());
			reg.setCountry(req.getCountry());
			reg.setState(req.getState());
			reg.setMobileNumber(req.getMobileNumber());
			reg.setAddressLine1(req.getAddressLine1());
			reg.setAddressLine2(req.getAddressLine2());
			registrationRepository.save(reg);
			if (userVerificationProperties.isRequireEmailVerification() && saved.getEmailVerifiedAt() == null) {
				sendVerificationEmail(saved);
			}
			log.info("Registration success userId={} email={}", saved.getId(), saved.getEmail());
			metrics.recordRegistration();
			return new UserResponse(saved.getId(), saved.getEmail(), saved.getCreatedAt());
		} catch (DataIntegrityViolationException ex) {
			// Covers race conditions with the unique index on email
			log.info("Registration rejected (unique constraint) email={}", email);
			throw new ResponseStatusException(HttpStatus.CONFLICT, "email already registered");
		}
	}

	public AuthResponse login(LoginRequest req) {
		String email = normalizeEmail(req.getEmail());
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials"));

		if (user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"This account uses Firebase sign-in. Use Google, Facebook, or email via Firebase.");
		}

		if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
			log.info("Login failed (bad password) email={}", email);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials");
		}

		if (userVerificationProperties.isRequireEmailVerification() && user.getEmailVerifiedAt() == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Please verify your email before logging in");
		}

		return issueSession(user, false);
	}

	/**
	 * Verifies a Firebase ID token from the SPA, creates or loads the user, returns JWTs.
	 */
	public AuthResponse firebaseRegisterOrLogin(FirebaseAuthRequest req) {
		if (!firebaseIdTokenVerifier.isConfigured()) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Firebase authentication is not configured on the server");
		}
		String raw = req.getIdToken() != null ? req.getIdToken().trim() : "";
		if (raw.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "idToken is required");
		}
		FirebaseIdentity identity;
		try {
			identity = firebaseIdTokenVerifier.verify(raw);
		} catch (FirebaseAuthException e) {
			log.warn("Invalid Firebase token: {}", e.getMessage());
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired Firebase token");
		}
		if (identity.email() == null || identity.email().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Firebase did not provide an email address");
		}
		if (userVerificationProperties.isRequireEmailVerification() && !identity.emailVerified()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"Please verify your email with Firebase before signing in");
		}
		String email = normalizeEmail(identity.email());
		Optional<User> byUid = userRepository.findByFirebaseUid(identity.uid());
		if (byUid.isPresent()) {
			User u = byUid.get();
			if (userVerificationProperties.isRequireEmailVerification() && u.getEmailVerifiedAt() == null) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Please verify your email before logging in");
			}
			return issueSession(u, false);
		}
		Optional<User> byEmail = userRepository.findByEmail(email);
		if (byEmail.isPresent()) {
			User existing = byEmail.get();
			if (existing.getAuthProvider() == AuthProvider.LOCAL && existing.getPasswordHash() != null) {
				throw new ResponseStatusException(HttpStatus.CONFLICT,
						"An account with this email already exists. Sign in with email and password below.");
			}
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"An account with this email already exists. Sign in with Firebase.");
		}
		User user = new User(UUID.randomUUID(), email, null, Instant.now());
		user.setAuthProvider(AuthProvider.FIREBASE);
		user.setFirebaseUid(identity.uid());
		user.setEmailVerifiedAt(identity.emailVerified() ? Instant.now() : null);
		String[] parts = splitDisplayName(identity.displayName());
		try {
			User saved = userRepository.save(user);
			Registration reg = new Registration(saved.getId(), "FIREBASE", false, null);
			if (parts[0] != null) {
				reg.setFirstName(parts[0]);
			}
			if (parts[1] != null) {
				reg.setLastName(parts[1]);
			}
			registrationRepository.save(reg);
			log.info("Firebase registration userId={} email={}", saved.getId(), email);
			return issueSession(saved, true);
		} catch (DataIntegrityViolationException ex) {
			log.info("Firebase registration conflict email={}", email);
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"An account with this email already exists. Sign in with Firebase or email/password.");
		}
	}

	private AuthResponse issueSession(User user, boolean newRegistration) {
		user.setLastLoginDate(Instant.now());
		userRepository.save(user);
		log.info("Login success userId={} email={}", user.getId(), user.getEmail());
		auditService.record(user.getId(), AuditAction.LOGIN);
		if (newRegistration) {
			metrics.recordRegistration();
		} else {
			metrics.recordLogin();
		}
		String accessToken = jwtService.createAccessToken(user);
		String refreshTokenValue = jwtService.createRefreshTokenValue();
		Instant expiresAt = Instant.now().plusSeconds(jwtService.getRefreshTokenTtlSeconds());
		RefreshToken refreshToken = new RefreshToken(
				UUID.randomUUID(),
				user.getId(),
				JwtService.hashToken(refreshTokenValue),
				expiresAt,
				Instant.now());
		refreshTokenRepository.save(refreshToken);
		return new AuthResponse(accessToken, refreshTokenValue, new UserResponse(user.getId(), user.getEmail(), user.getCreatedAt()));
	}

	private static String[] splitDisplayName(String name) {
		if (name == null || name.isBlank()) {
			return new String[] { null, null };
		}
		String[] p = name.trim().split("\\s+", 2);
		if (p.length == 1) {
			return new String[] { p[0], null };
		}
		return new String[] { p[0], p[1] };
	}

	public AuthResponse refresh(String refreshTokenValue) {
		String hash = JwtService.hashToken(refreshTokenValue);
		RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid refresh token"));
		if (stored.getExpiresAt().isBefore(Instant.now())) {
			refreshTokenRepository.delete(stored);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "refresh token expired");
		}
		User user = userRepository.findById(stored.getUserId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not found"));
		refreshTokenRepository.delete(stored);
		String newAccessToken = jwtService.createAccessToken(user);
		String newRefreshValue = jwtService.createRefreshTokenValue();
		Instant expiresAt = Instant.now().plusSeconds(jwtService.getRefreshTokenTtlSeconds());
		RefreshToken newToken = new RefreshToken(
				UUID.randomUUID(),
				user.getId(),
				JwtService.hashToken(newRefreshValue),
				expiresAt,
				Instant.now());
		refreshTokenRepository.save(newToken);
		metrics.recordRefreshToken();
		return new AuthResponse(newAccessToken, newRefreshValue, new UserResponse(user.getId(), user.getEmail(), user.getCreatedAt()));
	}

	public void verifyUserByToken(String verificationToken) {
		User user = userRepository.findByEmailVerificationToken(verificationToken)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "invalid or expired verification link"));
		if (user.getEmailVerifiedAt() != null) {
			throw new ResponseStatusException(HttpStatus.GONE, "already verified");
		}
		if (user.getEmailVerificationTokenExpiresAt() != null
				&& user.getEmailVerificationTokenExpiresAt().isBefore(Instant.now())) {
			throw new ResponseStatusException(HttpStatus.GONE, "verification link expired");
		}
		user.setEmailVerifiedAt(Instant.now());
		user.setEmailVerificationToken(null);
		user.setEmailVerificationTokenExpiresAt(null);
		userRepository.save(user);
		log.info("User {} verified email", user.getId());
	}

	private void sendVerificationEmail(User user) {
		if (!emailProperties.isEnabled()) {
			log.debug("Email disabled, skipping user verification email for: {}", user.getId());
			return;
		}
		String baseUrl = emailProperties.getAppBaseUrl();
		if (baseUrl == null || baseUrl.isBlank()) {
			log.warn("APP_BASE_URL not set, cannot send verification email to user: {}", user.getId());
			return;
		}
		String verifyUrl = baseUrl.replaceAll("/$", "") + "/api/user/verify-email/" + user.getEmailVerificationToken();
		String htmlBody = userVerificationTemplate.render(user.getEmail(), verifyUrl);
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
			helper.setFrom(emailProperties.getFromAddress());
			helper.setTo(user.getEmail());
			helper.setSubject(emailProperties.getSubjectPrefix() + " Verify your email");
			helper.setText(htmlBody, true);
			mailSender.send(message);
			log.info("Sent verification email to user: {} ({})", user.getId(), user.getEmail());
		} catch (MessagingException e) {
			log.error("Failed to send verification email to user: {}", user.getId(), e);
		}
	}

	private static String normalizeEmail(String email) {
		return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}
}
