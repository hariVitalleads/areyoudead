package com.areyoudead.login;

import com.areyoudead.audit.AuditAction;
import com.areyoudead.audit.AuditService;
import com.areyoudead.login.dto.AccountDetailsResponse;
import com.areyoudead.login.dto.UpdateDetailsRequest;
import com.areyoudead.registration.Registration;
import com.areyoudead.registration.RegistrationRepository;
import com.areyoudead.user.User;
import com.areyoudead.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LoginService {
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final HexFormat HEX = HexFormat.of();

	private final UserRepository userRepository;
	private final RegistrationRepository registrationRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuditService auditService;

	public LoginService(
		UserRepository userRepository,
		RegistrationRepository registrationRepository,
		PasswordEncoder passwordEncoder,
		AuditService auditService
	) {
		this.userRepository = userRepository;
		this.registrationRepository = registrationRepository;
		this.passwordEncoder = passwordEncoder;
		this.auditService = auditService;
	}

	public AccountDetailsResponse getAccount(UUID userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
		Registration reg = registrationRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "registration not found"));
		return toAccountDetails(user, reg);
	}

	@Transactional
	public AccountDetailsResponse updateDetails(UUID userId, UpdateDetailsRequest req) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));

		Registration reg = registrationRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "registration not found"));

		if (req.getEmail() != null && !req.getEmail().isBlank()) {
			String email = normalizeEmail(req.getEmail());
			if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
				throw new ResponseStatusException(HttpStatus.CONFLICT, "email already registered");
			}
			user.setEmail(email);
		}

		if (req.getRegistrationType() != null && !req.getRegistrationType().isBlank()) {
			reg.setRegistrationType(req.getRegistrationType().trim());
		}

		if (req.getFirstName() != null && !req.getFirstName().isBlank()) {
			reg.setFirstName(req.getFirstName().trim());
		}
		if (req.getMiddleName() != null && !req.getMiddleName().isBlank()) {
			reg.setMiddleName(req.getMiddleName().trim());
		}
		if (req.getLastName() != null && !req.getLastName().isBlank()) {
			reg.setLastName(req.getLastName().trim());
		}
		if (req.getCountry() != null && !req.getCountry().isBlank()) {
			reg.setCountry(req.getCountry().trim());
		}
		if (req.getState() != null && !req.getState().isBlank()) {
			reg.setState(req.getState().trim());
		}
		if (req.getMobileNumber() != null && !req.getMobileNumber().isBlank()) {
			reg.setMobileNumber(req.getMobileNumber().trim());
		}
		if (req.getAddressLine1() != null && !req.getAddressLine1().isBlank()) {
			reg.setAddressLine1(req.getAddressLine1().trim());
		}
		if (req.getAddressLine2() != null && !req.getAddressLine2().isBlank()) {
			reg.setAddressLine2(req.getAddressLine2().trim());
		}

		try {
			User savedUser = userRepository.save(user);
			Registration savedReg = registrationRepository.save(reg);
			auditService.record(userId, AuditAction.UPDATE_DETAILS, "updated login/details");
			return toAccountDetails(savedUser, savedReg);
		} catch (DataIntegrityViolationException ex) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "email already registered");
		}
	}

	/**
	 * Creates a reset token and stores only its SHA-256 hash on the user.
	 * Returns the raw token (caller can email it; for dev we return it to client).
	 */
	@Transactional
	public String forgotPassword(String email) {
		String normalized = normalizeEmail(email);
		var userOpt = userRepository.findByEmail(normalized);
		if (userOpt.isEmpty()) {
			return null;
		}

		User user = userOpt.get();
		String token = generateToken();
		user.setPasswordResetTokenHash(sha256Hex(token));
		user.setPasswordResetExpiresAt(Instant.now().plusSeconds(30 * 60)); // 30 minutes
		userRepository.save(user);
		auditService.record(user.getId(), AuditAction.PASSWORD_RESET_REQUESTED);
		return token;
	}

	@Transactional
	public void resetPassword(String token, String newPassword) {
		String tokenHash = sha256Hex(token);
		User user = userRepository
			.findByPasswordResetTokenHashAndPasswordResetExpiresAtAfter(tokenHash, Instant.now())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid or expired token"));

		user.setPasswordHash(passwordEncoder.encode(newPassword));
		user.setPasswordResetTokenHash(null);
		user.setPasswordResetExpiresAt(null);
		userRepository.save(user);
		auditService.record(user.getId(), AuditAction.PASSWORD_RESET_COMPLETED);
	}

	private static AccountDetailsResponse toAccountDetails(User user, Registration reg) {
		return new AccountDetailsResponse(
			user.getId(),
			user.getEmail(),
			user.getCreatedAt(),
			user.getLastLoginDate(),
			reg.getRegistrationType(),
			reg.getFirstName(),
			reg.getMiddleName(),
			reg.getLastName(),
			reg.getCountry(),
			reg.getState(),
			reg.getMobileNumber(),
			reg.getAddressLine1(),
			reg.getAddressLine2(),
			reg.isHasPaid(),
			reg.getPaidAt()
		);
	}

	private static String normalizeEmail(String email) {
		return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}

	private static String generateToken() {
		byte[] bytes = new byte[32];
		SECURE_RANDOM.nextBytes(bytes);
		return HEX.formatHex(bytes);
	}

	private static String sha256Hex(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			return HEX.formatHex(hashed);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}
}

