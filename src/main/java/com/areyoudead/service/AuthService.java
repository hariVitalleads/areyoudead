package com.areyoudead.service;

import com.areyoudead.audit.AuditAction;
import com.areyoudead.dto.AuthResponse;
import com.areyoudead.dto.LoginRequest;
import com.areyoudead.dto.RegisterRequest;
import com.areyoudead.dto.UserResponse;
import com.areyoudead.model.Registration;
import com.areyoudead.repository.RegistrationRepository;
import com.areyoudead.security.JwtService;
import com.areyoudead.model.User;
import com.areyoudead.repository.UserRepository;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	private final UserRepository userRepository;
	private final RegistrationRepository registrationRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuditService auditService;

	public AuthService(
			UserRepository userRepository,
			RegistrationRepository registrationRepository,
			PasswordEncoder passwordEncoder,
			JwtService jwtService,
			AuditService auditService) {
		this.userRepository = userRepository;
		this.registrationRepository = registrationRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.auditService = auditService;
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
			log.info("Registration success userId={} email={}", saved.getId(), saved.getEmail());
			log.info("Registration success userId={} email={}", saved.getId(), saved.getEmail());
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

		user.setLastLoginDate(Instant.now());
		userRepository.save(user);
		log.info("Login success userId={} email={}", user.getId(), user.getEmail());
		auditService.record(user.getId(), AuditAction.LOGIN);

		String token = jwtService.createAccessToken(user);
		return new AuthResponse(token, new UserResponse(user.getId(), user.getEmail(), user.getCreatedAt()));
	}

	private static String normalizeEmail(String email) {
		return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}
}
