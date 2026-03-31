package com.checkin.repository;

import com.checkin.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, UUID> {
	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	Optional<User> findByPasswordResetTokenHashAndPasswordResetExpiresAtAfter(String passwordResetTokenHash,
			Instant now);

	List<User> findByLastLoginDateBefore(Instant date);

	Optional<User> findByEmailVerificationToken(String emailVerificationToken);

	/** For admin audit: list all users, newest first. */
	List<User> findAllByOrderByCreatedAtDesc();

	@Query("SELECT u FROM User u WHERE u.fcmToken IS NOT NULL AND u.fcmToken != '' AND u.notificationTimesJson IS NOT NULL AND u.notificationTimesJson != ''")
	List<User> findAllWithFcmTokenAndSchedule();
}
