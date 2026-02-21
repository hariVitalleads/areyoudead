package com.checkin.service;

import com.checkin.model.User;
import com.checkin.config.AppMetrics;
import com.checkin.repository.UserRepository;
import com.checkin.audit.AuditAction;
import com.checkin.dto.AppUserDetailsResponse;
import com.checkin.dto.UpdateAppUserRequest;

import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AppUserService {
	private final UserRepository userRepository;
	private final AuditService auditService;
	private final AppMetrics metrics;

	public AppUserService(UserRepository userRepository, AuditService auditService, AppMetrics metrics) {
		this.userRepository = userRepository;
		this.auditService = auditService;
		this.metrics = metrics;
	}

	public AppUserDetailsResponse me(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
		return toResponse(user);
	}

	/**
	 * Manual check-in ("I'm okay") — updates lastManualCheckInAt and optionally snoozes alerts.
	 * When snoozeDays is set, alerts are snoozed until that many days from now.
	 */
	@Transactional
	public void checkIn(UUID userId, Integer snoozeDays) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
		java.time.Instant now = java.time.Instant.now();
		user.setLastManualCheckInAt(now);
		user.setFirstAlertSentAt(null);
		user.setContactsAlertedCount(null);
		if (snoozeDays != null && snoozeDays > 0) {
			user.setAlertsSnoozedUntil(now.plusSeconds(snoozeDays * 86_400L));
			auditService.record(userId, AuditAction.CHECK_IN, "check-in with snooze " + snoozeDays + " days");
		} else {
			user.setAlertsSnoozedUntil(null);
			auditService.record(userId, AuditAction.CHECK_IN, "manual check-in");
		}
		userRepository.save(user);
		metrics.recordCheckIn();
	}

	@Transactional
	public AppUserDetailsResponse update(UUID userId, UpdateAppUserRequest req) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));

		if (req.getEmail() != null && !req.getEmail().isBlank()) {
			String normalized = normalizeEmail(req.getEmail());
			if (!normalized.equals(user.getEmail()) && userRepository.existsByEmail(normalized)) {
				throw new ResponseStatusException(HttpStatus.CONFLICT, "email already registered");
			}
			user.setEmail(normalized);
		}
		if (req.getInactivityThresholdDays() != null) {
			user.setInactivityThresholdDays(req.getInactivityThresholdDays());
		}

		try {
			User saved = userRepository.save(user);
			auditService.record(userId, AuditAction.UPDATE_DETAILS, "updated user/details");
			return toResponse(saved);
		} catch (DataIntegrityViolationException ex) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "email already registered");
		}
	}

	private static AppUserDetailsResponse toResponse(User user) {
		return new AppUserDetailsResponse(user.getId(), user.getEmail(), user.getCreatedAt(), user.getLastLoginDate(), user.getInactivityThresholdDays());
	}

	private static String normalizeEmail(String email) {
		return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}
}
