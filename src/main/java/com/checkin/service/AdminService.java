package com.checkin.service;

import com.checkin.dto.AdminUserAuditResponse;
import com.checkin.dto.AdminUserSummaryResponse;
import com.checkin.dto.AuditEventResponse;
import com.checkin.dto.EmergencyContactResponse;
import com.checkin.model.User;
import com.checkin.repository.AuditEventRepository;
import com.checkin.repository.EmergencyContactRepository;
import com.checkin.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service for super-user audit operations. Access restricted to users with ROLE_SUPER_USER.
 */
@Service
public class AdminService {
	private final UserRepository userRepository;
	private final AuditEventRepository auditEventRepository;
	private final EmergencyContactRepository emergencyContactRepository;

	public AdminService(UserRepository userRepository, AuditEventRepository auditEventRepository,
			EmergencyContactRepository emergencyContactRepository) {
		this.userRepository = userRepository;
		this.auditEventRepository = auditEventRepository;
		this.emergencyContactRepository = emergencyContactRepository;
	}

	/**
	 * List all users for audit purposes, newest first. Excludes password and other sensitive data.
	 */
	public List<AdminUserSummaryResponse> listUsers() {
		return userRepository.findAllByOrderByCreatedAtDesc().stream()
				.map(this::toSummary)
				.toList();
	}

	/**
	 * Full audit view for a single user: details, audit events, emergency contacts.
	 */
	public AdminUserAuditResponse getUserAuditDetail(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));

		List<AuditEventResponse> events = auditEventRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
				.map(e -> new AuditEventResponse(e.getId(), e.getAction(), e.getDetails(), e.getCreatedAt()))
				.toList();

		List<EmergencyContactResponse> contacts = emergencyContactRepository
				.findByUserIdOrderByContactIndexAsc(userId).stream()
				.map(c -> new EmergencyContactResponse(c.getId(), c.getMobileNumber(), c.getEmail(),
						c.getContactIndex(), c.getVerifiedAt() != null, c.getLabel()))
				.toList();

		return new AdminUserAuditResponse(
				user.getId(),
				user.getEmail(),
				user.getCreatedAt(),
				user.getLastLoginDate(),
				user.getLastManualCheckInAt(),
				user.getFirstAlertSentAt(),
				user.getContactsAlertedCount() != null ? user.getContactsAlertedCount().intValue() : null,
				user.getInactivityThresholdDays() != null ? user.getInactivityThresholdDays().intValue() : null,
				user.getAlertChannelPreference(),
				events,
				contacts);
	}

	private AdminUserSummaryResponse toSummary(User u) {
		Integer threshold = u.getInactivityThresholdDays() != null ? u.getInactivityThresholdDays().intValue() : null;
		return new AdminUserSummaryResponse(
				u.getId(),
				u.getEmail(),
				u.getCreatedAt(),
				u.getLastLoginDate(),
				u.getLastManualCheckInAt(),
				threshold,
				u.getAlertChannelPreference());
	}
}
