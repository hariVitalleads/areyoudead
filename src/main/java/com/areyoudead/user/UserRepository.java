package com.areyoudead.user;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
	Optional<User> findByEmail(String email);
	boolean existsByEmail(String email);
	Optional<User> findByPasswordResetTokenHashAndPasswordResetExpiresAtAfter(String passwordResetTokenHash, Instant now);
}

