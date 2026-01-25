package com.areyoudead.repository;
import com.areyoudead.model.AuditEvent;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {}

