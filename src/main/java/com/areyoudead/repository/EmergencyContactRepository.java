package com.areyoudead.repository;
import com.areyoudead.model.EmergencyContact;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, UUID> {
	List<EmergencyContact> findByUserIdOrderByContactIndexAsc(UUID userId);
}

