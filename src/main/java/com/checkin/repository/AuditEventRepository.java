package com.checkin.repository;
import com.checkin.model.AuditEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

	List<AuditEvent> findByUserIdOrderByCreatedAtDesc(UUID userId);

	long countByUserIdAndAction(UUID userId, String action);

	Optional<AuditEvent> findTop1ByUserIdAndActionOrderByCreatedAtDesc(UUID userId, String action);

	@Query("SELECT COUNT(e) FROM AuditEvent e WHERE e.userId = :userId AND e.action = :action AND e.createdAt >= :since")
	long countByUserIdAndActionSince(@Param("userId") UUID userId, @Param("action") String action,
			@Param("since") Instant since);
}

