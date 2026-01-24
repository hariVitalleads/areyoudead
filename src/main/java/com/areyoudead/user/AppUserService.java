package com.areyoudead.user;

import com.areyoudead.audit.AuditAction;
import com.areyoudead.audit.AuditService;
import com.areyoudead.user.dto.AppUserDetailsResponse;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AppUserService {
	private final UserRepository userRepository;
	private final AuditService auditService;

	public AppUserService(UserRepository userRepository, AuditService auditService) {
		this.userRepository = userRepository;
		this.auditService = auditService;
	}

	public AppUserDetailsResponse me(UUID userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
		return toResponse(user);
	}

	@Transactional
	public AppUserDetailsResponse update(UUID userId, String email) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));

		if (email != null && !email.isBlank()) {
			String normalized = normalizeEmail(email);
			if (!normalized.equals(user.getEmail()) && userRepository.existsByEmail(normalized)) {
				throw new ResponseStatusException(HttpStatus.CONFLICT, "email already registered");
			}
			user.setEmail(normalized);
		}

		try {
			User saved = userRepository.save(user);
			auditService.record(userId, AuditAction.UPDATE_DETAILS, "updated user/details");
			return toResponse(saved);
		} catch (DataIntegrityViolationException ex) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "email already registered");
		}
	}

	private static AppUserDetailsResponse toResponse(User user) {
		return new AppUserDetailsResponse(user.getId(), user.getEmail(), user.getCreatedAt(), user.getLastLoginDate());
	}

	private static String normalizeEmail(String email) {
		return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}
}

