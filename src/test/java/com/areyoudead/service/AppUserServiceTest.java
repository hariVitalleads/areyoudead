package com.areyoudead.service;

import com.areyoudead.audit.AuditAction;
import com.areyoudead.dto.AppUserDetailsResponse;
import com.areyoudead.model.User;
import com.areyoudead.repository.UserRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

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
        String newEmail = "newemail@example.com";
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail(newEmail.toLowerCase())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        AppUserDetailsResponse response = appUserService.update(userId, newEmail);

        // Then
        assertNotNull(response);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(newEmail.toLowerCase(), userCaptor.getValue().getEmail());
        verify(auditService).record(userId, AuditAction.UPDATE_DETAILS, "updated user/details");
    }

    @Test
    void update_Success_WithNullEmail() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        AppUserDetailsResponse response = appUserService.update(userId, null);

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
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        AppUserDetailsResponse response = appUserService.update(userId, "   ");

        // Then
        assertNotNull(response);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("test@example.com", userCaptor.getValue().getEmail()); // unchanged
    }

    @Test
    void update_SameEmail_NoConflict() {
        // Given
        String sameEmail = "test@example.com";
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        AppUserDetailsResponse response = appUserService.update(userId, sameEmail);

        // Then
        assertNotNull(response);
        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    void update_EmailAlreadyExists_ThrowsConflict() {
        // Given
        String newEmail = "existing@example.com";
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail(newEmail.toLowerCase())).thenReturn(true);

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> appUserService.update(userId, newEmail));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("email already registered", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void update_DataIntegrityViolation_ThrowsConflict() {
        // Given
        String newEmail = "newemail@example.com";
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail(newEmail.toLowerCase())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("Duplicate"));

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> appUserService.update(userId, newEmail));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("email already registered", exception.getReason());
    }

    @Test
    void update_UserNotFound_ThrowsNotFound() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> appUserService.update(userId, "newemail@example.com"));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("user not found", exception.getReason());
    }

    @Test
    void update_EmailNormalized() {
        // Given
        String newEmail = "  NEWEMAIL@EXAMPLE.COM  ";
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        appUserService.update(userId, newEmail);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("newemail@example.com", userCaptor.getValue().getEmail());
    }
}
