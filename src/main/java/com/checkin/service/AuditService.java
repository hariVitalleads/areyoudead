package com.checkin.service;

import com.checkin.audit.AuditAction;
import com.checkin.model.AuditEvent;
import com.checkin.repository.AuditEventRepository;

import java.time.Instant;
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
}
