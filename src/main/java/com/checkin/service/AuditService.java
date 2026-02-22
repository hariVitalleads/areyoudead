package com.checkin.service;

import com.checkin.audit.AuditAction;
import com.checkin.dto.AuditEventResponse;
import com.checkin.model.AuditEvent;
import com.checkin.repository.AuditEventRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
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
}
