package com.checkin.repository;

import com.checkin.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
	Optional<User> findByEmail(String email);

	Optional<User> findByFirebaseUid(String firebaseUid);

	boolean existsByEmail(String email);

	Optional<User> findByPasswordResetTokenHashAndPasswordResetExpiresAtAfter(String passwordResetTokenHash,
			Instant now);

	List<User> findByLastLoginDateBefore(Instant date);

	Optional<User> findByEmailVerificationToken(String emailVerificationToken);

	/** For admin audit: list all users, newest first. */
	List<User> findAllByOrderByCreatedAtDesc();
}
