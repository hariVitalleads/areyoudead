package com.areyoudead.service;

import com.areyoudead.audit.AuditAction;
import com.areyoudead.dto.AccountDetailsResponse;
import com.areyoudead.dto.UpdateDetailsRequest;
import com.areyoudead.model.Registration;
import com.areyoudead.model.User;
import com.areyoudead.repository.RegistrationRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private LoginService loginService;

    private UUID userId;
    private User user;
    private Registration registration;
    private Instant createdAt;
    private Instant lastLoginDate;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        createdAt = Instant.now();
        lastLoginDate = Instant.now().minusSeconds(3600);
        user = new User(userId, "test@example.com", "hashedPassword", createdAt);
        user.setLastLoginDate(lastLoginDate);
        registration = new Registration(userId, "STANDARD", false, null);
        registration.setFirstName("John");
        registration.setLastName("Doe");
    }

    @Test
    void getAccount_Success() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(registrationRepository.findById(userId)).thenReturn(Optional.of(registration));

        // When
        AccountDetailsResponse response = loginService.getAccount(userId);

        // Then
        assertNotNull(response);
        assertEquals(userId, response.getId());
        assertEquals("test@example.com", response.getEmail());
        assertEquals(createdAt, response.getCreatedAt());
        assertEquals(lastLoginDate, response.getLastLoginDate());
        assertEquals("STANDARD", response.getRegistrationType());
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
    }

    @Test
    void getAccount_UserNotFound_ThrowsNotFound() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> loginService.getAccount(userId));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("user not found", exception.getReason());
    }

    @Test
    void getAccount_RegistrationNotFound_ThrowsNotFound() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(registrationRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> loginService.getAccount(userId));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("registration not found", exception.getReason());
    }

    @Test
    void updateDetails_Success_AllFields() {
        // Given
        UpdateDetailsRequest request = new UpdateDetailsRequest();
        request.setEmail("newemail@example.com");
        request.setFirstName("Jane");
        request.setLastName("Smith");
        request.setCountry("USA");
        request.setState("CA");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(registrationRepository.findById(userId)).thenReturn(Optional.of(registration));
        when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(registrationRepository.save(any(Registration.class))).thenReturn(registration);

        // When
        AccountDetailsResponse response = loginService.updateDetails(userId, request);

        // Then
        assertNotNull(response);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<Registration> regCaptor = ArgumentCaptor.forClass(Registration.class);
        verify(userRepository).save(userCaptor.capture());
        verify(registrationRepository).save(regCaptor.capture());
        assertEquals("newemail@example.com", userCaptor.getValue().getEmail());
        assertEquals("Jane", regCaptor.getValue().getFirstName());
        assertEquals("Smith", regCaptor.getValue().getLastName());
        assertEquals("USA", regCaptor.getValue().getCountry());
        assertEquals("CA", regCaptor.getValue().getState());
        verify(auditService).record(userId, AuditAction.UPDATE_DETAILS, "updated login/details");
    }

    @Test
    void updateDetails_Success_PartialUpdate() {
        // Given
        UpdateDetailsRequest request = new UpdateDetailsRequest();
        request.setFirstName("Jane");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(registrationRepository.findById(userId)).thenReturn(Optional.of(registration));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(registrationRepository.save(any(Registration.class))).thenReturn(registration);

        // When
        loginService.updateDetails(userId, request);

        // Then
        ArgumentCaptor<Registration> regCaptor = ArgumentCaptor.forClass(Registration.class);
        verify(registrationRepository).save(regCaptor.capture());
        assertEquals("Jane", regCaptor.getValue().getFirstName());
        assertEquals("Doe", regCaptor.getValue().getLastName()); // unchanged
    }

    @Test
    void updateDetails_EmailAlreadyExists_ThrowsConflict() {
        // Given
        UpdateDetailsRequest request = new UpdateDetailsRequest();
        request.setEmail("existing@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(registrationRepository.findById(userId)).thenReturn(Optional.of(registration));
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> loginService.updateDetails(userId, request));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("email already registered", exception.getReason());
    }

    @Test
    void updateDetails_DataIntegrityViolation_ThrowsConflict() {
        // Given
        UpdateDetailsRequest request = new UpdateDetailsRequest();
        request.setEmail("newemail@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(registrationRepository.findById(userId)).thenReturn(Optional.of(registration));
        when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("Duplicate"));

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> loginService.updateDetails(userId, request));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("email already registered", exception.getReason());
    }

    @Test
    void updateDetails_BlankFieldsIgnored() {
        // Given
        UpdateDetailsRequest request = new UpdateDetailsRequest();
        request.setFirstName("   ");
        request.setLastName("");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(registrationRepository.findById(userId)).thenReturn(Optional.of(registration));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(registrationRepository.save(any(Registration.class))).thenReturn(registration);

        // When
        loginService.updateDetails(userId, request);

        // Then
        ArgumentCaptor<Registration> regCaptor = ArgumentCaptor.forClass(Registration.class);
        verify(registrationRepository).save(regCaptor.capture());
        assertEquals("John", regCaptor.getValue().getFirstName()); // unchanged
        assertEquals("Doe", regCaptor.getValue().getLastName()); // unchanged
    }

    @Test
    void forgotPassword_Success() {
        // Given
        String email = "test@example.com";
        when(userRepository.findByEmail(email.toLowerCase())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        String token = loginService.forgotPassword(email);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertNotNull(userCaptor.getValue().getPasswordResetTokenHash());
        assertNotNull(userCaptor.getValue().getPasswordResetExpiresAt());
        verify(auditService).record(userId, AuditAction.PASSWORD_RESET_REQUESTED);
    }

    @Test
    void forgotPassword_UserNotFound_ReturnsNull() {
        // Given
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email.toLowerCase())).thenReturn(Optional.empty());

        // When
        String token = loginService.forgotPassword(email);

        // Then
        assertNull(token);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_Success() {
        // Given
        String token = "valid-token";
        String newPassword = "newPassword123";
        String tokenHash = "hashed-token";
        user.setPasswordResetTokenHash(tokenHash);
        user.setPasswordResetExpiresAt(Instant.now().plusSeconds(1800));

        when(userRepository.findByPasswordResetTokenHashAndPasswordResetExpiresAtAfter(
                anyString(), any(Instant.class))).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(newPassword)).thenReturn("newHashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        loginService.resetPassword(token, newPassword);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertNull(userCaptor.getValue().getPasswordResetTokenHash());
        assertNull(userCaptor.getValue().getPasswordResetExpiresAt());
        verify(auditService).record(userId, AuditAction.PASSWORD_RESET_COMPLETED);
    }

    @Test
    void resetPassword_InvalidOrExpiredToken_ThrowsBadRequest() {
        // Given
        String token = "invalid-token";
        String newPassword = "newPassword123";

        when(userRepository.findByPasswordResetTokenHashAndPasswordResetExpiresAtAfter(
                anyString(), any(Instant.class))).thenReturn(Optional.empty());

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> loginService.resetPassword(token, newPassword));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("invalid or expired token", exception.getReason());
        verify(passwordEncoder, never()).encode(anyString());
    }
}
