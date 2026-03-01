package com.checkin.service;

import com.checkin.audit.AuditAction;
import com.checkin.dto.AuditEventResponse;
import com.checkin.dto.CheckInSummaryResponse;
import com.checkin.model.AuditEvent;
import com.checkin.repository.AuditEventRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
	private static final String CHECK_IN = AuditAction.CHECK_IN.name();

	private final AuditEventRepository auditEventRepository;

	public AuditService(AuditEventRepository auditEventRepository) {
		this.auditEventRepository = auditEventRepository;
	}

	public void record(UUID userId, AuditAction action) {
		record(userId, action, null);
	}

	public void record(UUID userId, AuditAction action, String details) {
		AuditEvent event = new AuditEvent(
				UUID.randomUUID(),
				userId,
				action.name(),
				details,
				Instant.now());
		auditEventRepository.save(event);
	}

	/**
	 * List audit events for the given user (their own records only), newest first.
	 */
	public List<AuditEventResponse> listForUser(UUID userId) {
		return auditEventRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
				.map(e -> new AuditEventResponse(e.getId(), e.getAction(), e.getDetails(), e.getCreatedAt()))
				.toList();
	}

	/**
	 * Check-in summary from audit_events: total count, last check-in, and counts for last 7/30 days.
	 */
	public CheckInSummaryResponse getCheckInSummary(UUID userId) {
		long total = auditEventRepository.countByUserIdAndAction(userId, CHECK_IN);
		Instant last = auditEventRepository.findTop1ByUserIdAndActionOrderByCreatedAtDesc(userId, CHECK_IN)
				.map(AuditEvent::getCreatedAt)
				.orElse(null);
		Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
		Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
		long last7 = auditEventRepository.countByUserIdAndActionSince(userId, CHECK_IN, sevenDaysAgo);
		long last30 = auditEventRepository.countByUserIdAndActionSince(userId, CHECK_IN, thirtyDaysAgo);
		return new CheckInSummaryResponse(total, last, last7, last30);
	}
}
