package com.checkin.service;

import com.checkin.model.Registration;
import com.checkin.model.User;
import com.checkin.config.AppMetrics;
import com.checkin.repository.RegistrationRepository;
import com.checkin.repository.UserRepository;
import com.checkin.audit.AuditAction;
import com.checkin.dto.AppUserDetailsResponse;
import com.checkin.dto.UpdateAppUserRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AppUserService {
	private final UserRepository userRepository;
	private final RegistrationRepository registrationRepository;
	private final AuditService auditService;
	private final AppMetrics metrics;

	public AppUserService(UserRepository userRepository, RegistrationRepository registrationRepository,
			AuditService auditService, AppMetrics metrics) {
		this.userRepository = userRepository;
		this.registrationRepository = registrationRepository;
		this.auditService = auditService;
		this.metrics = metrics;
	}

	public AppUserDetailsResponse me(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
		Optional<Registration> reg = registrationRepository.findById(userId);
		return toResponse(user, reg.orElse(null));
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
			user.setInactivityThresholdDays((short) req.getInactivityThresholdDays().intValue());
		}
		if (req.getAlertChannelPreference() != null) {
			user.setAlertChannelPreference(req.getAlertChannelPreference());
		}

		Registration reg = registrationRepository.findById(userId).orElse(null);
		if (reg != null) {
			if (req.getFirstName() != null) reg.setFirstName(req.getFirstName());
			if (req.getLastName() != null) reg.setLastName(req.getLastName());
			if (req.getMobileNumber() != null) reg.setMobileNumber(req.getMobileNumber());
		}
		if (req.getFcmToken() != null) {
			user.setFcmToken(req.getFcmToken().isBlank() ? null : req.getFcmToken().trim());
		}
		if (req.getNotificationTimes() != null) {
			if (req.getNotificationTimes().isEmpty()) {
				user.setNotificationTimesJson(null);
			} else {
				validateNotificationTimes(req.getNotificationTimes());
				user.setNotificationTimesJson(serializeNotificationTimes(req.getNotificationTimes()));
			}
		}

		try {
			User saved = userRepository.save(user);
			if (reg != null) registrationRepository.save(reg);
			auditService.record(userId, AuditAction.UPDATE_DETAILS, "updated user/details");
			return toResponse(saved, reg);
		} catch (DataIntegrityViolationException ex) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "email already registered");
		}
	}

	private static AppUserDetailsResponse toResponse(User user, Registration reg) {
		String fn = reg != null ? reg.getFirstName() : null;
		String ln = reg != null ? reg.getLastName() : null;
		String mob = reg != null ? reg.getMobileNumber() : null;
		Integer threshold = user.getInactivityThresholdDays() != null ? user.getInactivityThresholdDays().intValue() : null;
		List<String> notificationTimes = parseNotificationTimes(user.getNotificationTimesJson());
		return new AppUserDetailsResponse(user.getId(), user.getEmail(), user.getCreatedAt(), user.getLastLoginDate(),
				threshold, user.getAlertChannelPreference(), fn, ln, mob, user.getFcmToken(), notificationTimes);
	}

	private static void validateNotificationTimes(List<String> times) {
		if (times == null) return;
		if (times.size() > 5) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "notificationTimes: max 5 times allowed");
		}
		for (String t : times) {
			if (t == null || !t.matches("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "notificationTimes: invalid format, use HH:mm");
			}
		}
	}

	private static String serializeNotificationTimes(List<String> times) {
		if (times == null || times.isEmpty()) return null;
		try {
			return new ObjectMapper().writeValueAsString(times);
		} catch (Exception e) {
			return null;
		}
	}

	private static List<String> parseNotificationTimes(String json) {
		if (json == null || json.isBlank()) return null;
		try {
			return new ObjectMapper().readValue(json, new TypeReference<>() {});
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	private static String normalizeEmail(String email) {
		return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}
}
