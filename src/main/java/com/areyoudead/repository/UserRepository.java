package com.areyoudead.repository;

import com.areyoudead.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	Optional<User> findByPasswordResetTokenHashAndPasswordResetExpiresAtAfter(String passwordResetTokenHash,
			Instant now);

	List<User> findByLastLoginDateBefore(Instant date);
}
