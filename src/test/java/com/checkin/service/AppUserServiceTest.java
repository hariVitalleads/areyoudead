package com.checkin.service;

import com.checkin.audit.AuditAction;
import com.checkin.config.AppMetrics;
import com.checkin.dto.AppUserDetailsResponse;
import com.checkin.dto.UpdateAppUserRequest;
import com.checkin.model.User;
import com.checkin.repository.RegistrationRepository;
import com.checkin.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private AppMetrics metrics;

    @InjectMocks
    private AppUserService appUserService;

    private UUID userId;
    private User user;
    private Instant createdAt;
    private Instant lastLoginDate;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        createdAt = Instant.now();
        lastLoginDate = Instant.now().minusSeconds(3600);
        user = new User(userId, "test@example.com", "hashedPassword", createdAt);
        user.setLastLoginDate(lastLoginDate);
    }

    @Test
    void me_Success() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(registrationRepository.findById(userId)).thenReturn(Optional.empty());

        // When
        AppUserDetailsResponse response = appUserService.me(userId);

        // Then
        assertNotNull(response);
        assertEquals(userId, response.getId());
        assertEquals("test@example.com", response.getEmail());
        assertEquals(createdAt, response.getCreatedAt());
        assertEquals(lastLoginDate, response.getLastLoginDate());
        verify(userRepository).findById(userId);
    }

    @Test
    void me_UserNotFound_ThrowsNotFound() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> appUserService.me(userId));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("user not found", exception.getReason());
    }

    @Test
    void update_Success_WithEmail() {
        // Given
        UpdateAppUserRequest req = new UpdateAppUserRequest();
        req.setEmail("newemail@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(registrationRepository.findById(userId)).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        AppUserDetailsResponse response = appUserService.update(userId, req);

        // Then
        assertNotNull(response);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("newemail@example.com", userCaptor.getValue().getEmail());
        verify(auditService).record(userId, AuditAction.UPDATE_DETAILS, "updated user/details");
    }

    @Test
    void update_Success_WithNullEmail() {
        // Given
        UpdateAppUserRequest req = new UpdateAppUserRequest();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(registrationRepository.findById(userId)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        AppUserDetailsResponse response = appUserService.update(userId, req);

        // Then
        assertNotNull(response);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("test@example.com", userCaptor.getValue().getEmail()); // unchanged
        verify(auditService).record(userId, AuditAction.UPDATE_DETAILS, "updated user/details");
    }

    @Test
    void update_Success_WithBlankEmail() {
        // Given
        UpdateAppUserRequest req = new UpdateAppUserRequest();
        req.setEmail("   ");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(registrationRepository.findById(userId)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        AppUserDetailsResponse response = appUserService.update(userId, req);

        // Then
        assertNotNull(response);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("test@example.com", userCaptor.getValue().getEmail()); // unchanged
    }

    @Test
    void update_SameEmail_NoConflict() {
        // Given
        UpdateAppUserRequest req = new UpdateAppUserRequest();
        req.setEmail("test@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(registrationRepository.findById(userId)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        AppUserDetailsResponse response = appUserService.update(userId, req);

        // Then
        assertNotNull(response);
        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    void update_EmailAlreadyExists_ThrowsConflict() {
        // Given: throws before reaching registrationRepository
        UpdateAppUserRequest req = new UpdateAppUserRequest();
        req.setEmail("existing@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> appUserService.update(userId, req));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("email already registered", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void update_DataIntegrityViolation_ThrowsConflict() {
        // Given
        UpdateAppUserRequest req = new UpdateAppUserRequest();
        req.setEmail("newemail@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(registrationRepository.findById(userId)).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("Duplicate"));

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> appUserService.update(userId, req));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("email already registered", exception.getReason());
    }

    @Test
    void update_UserNotFound_ThrowsNotFound() {
        // Given
        UpdateAppUserRequest req = new UpdateAppUserRequest();
        req.setEmail("newemail@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> appUserService.update(userId, req));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("user not found", exception.getReason());
    }

    @Test
    void update_EmailNormalized() {
        // Given
        UpdateAppUserRequest req = new UpdateAppUserRequest();
        req.setEmail("  NEWEMAIL@EXAMPLE.COM  ");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(registrationRepository.findById(userId)).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        appUserService.update(userId, req);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("newemail@example.com", userCaptor.getValue().getEmail());
    }
}
