package com.checkin.service;

import com.checkin.audit.AuditAction;
import com.checkin.config.AppMetrics;
import com.checkin.config.EmailProperties;
import com.checkin.config.UserVerificationProperties;
import com.checkin.dto.AuthResponse;
import com.checkin.dto.LoginRequest;
import com.checkin.dto.RegisterRequest;
import com.checkin.dto.UserResponse;
import com.checkin.model.RefreshToken;
import com.checkin.model.Registration;
import com.checkin.model.User;
import com.checkin.repository.RefreshTokenRepository;
import com.checkin.repository.RegistrationRepository;
import com.checkin.repository.UserRepository;
import com.checkin.security.JwtService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.util.Locale;
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
			UserVerificationTemplate userVerificationTemplate) {
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

		if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
			log.info("Login failed (bad password) email={}", email);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials");
		}

		if (userVerificationProperties.isRequireEmailVerification() && user.getEmailVerifiedAt() == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Please verify your email before logging in");
		}

		user.setLastLoginDate(Instant.now());
		userRepository.save(user);
		log.info("Login success userId={} email={}", user.getId(), user.getEmail());
		auditService.record(user.getId(), AuditAction.LOGIN);
		metrics.recordLogin();

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
