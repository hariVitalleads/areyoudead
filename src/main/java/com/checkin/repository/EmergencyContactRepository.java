package com.checkin.repository;
import com.checkin.model.EmergencyContact;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, UUID> {
	List<EmergencyContact> findByUserIdOrderByContactIndexAsc(UUID userId);

	java.util.Optional<EmergencyContact> findByOptOutToken(java.util.UUID optOutToken);

	java.util.Optional<EmergencyContact> findByVerificationToken(String verificationToken);
}

