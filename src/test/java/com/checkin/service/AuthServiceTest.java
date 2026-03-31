package com.checkin.service;

import com.checkin.audit.AuditAction;
import com.checkin.dto.AuthResponse;
import com.checkin.dto.LoginRequest;
import com.checkin.dto.RegisterRequest;
import com.checkin.dto.UserResponse;
import com.checkin.config.AppMetrics;
import com.checkin.config.EmailProperties;
import com.checkin.config.UserVerificationProperties;
import com.checkin.model.RefreshToken;
import com.checkin.model.Registration;
import com.checkin.model.User;
import com.checkin.repository.RefreshTokenRepository;
import com.checkin.repository.RegistrationRepository;
import com.checkin.repository.UserRepository;
import com.checkin.security.JwtService;
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

import jakarta.mail.internet.MimeMessage;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuditService auditService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private AppMetrics metrics;

    @Mock
    private EmailProperties emailProperties;

    @Mock
    private UserVerificationProperties userVerificationProperties;

    @Mock
    private org.springframework.mail.javamail.JavaMailSender mailSender;

    @Mock
    private UserVerificationTemplate userVerificationTemplate;

    @InjectMocks
    private AuthService authService;

    private UUID userId;
    private String email;
    private String password;
    private User user;
    private Registration registration;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        email = "test@example.com";
        password = "password123";
        Instant createdAt = Instant.now();

        user = new User(userId, email, "hashedPassword", createdAt);
        user.setEmailVerifiedAt(Instant.now()); // Required for login when verification is enforced
        registration = new Registration(userId, "STANDARD", false, null);

        org.mockito.Mockito.lenient().when(userVerificationProperties.isRequireEmailVerification()).thenReturn(false);
    }

    @Test
    void register_Success() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword(password);
        request.setFirstName("John");
        request.setLastName("Doe");

        when(userRepository.existsByEmail(email.toLowerCase())).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(registrationRepository.save(any(Registration.class))).thenReturn(registration);

        // When
        UserResponse response = authService.register(request);

        // Then
        assertNotNull(response);
        assertEquals(userId, response.getId());
        assertEquals(email, response.getEmail());
        verify(userRepository).existsByEmail(email.toLowerCase());
        verify(userRepository).save(any(User.class));
        verify(registrationRepository).save(any(Registration.class));
    }

    @Test
    void register_EmailAlreadyExists_ThrowsConflict() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword(password);

        when(userRepository.existsByEmail(email.toLowerCase())).thenReturn(true);

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.register(request));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("email already registered", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_DataIntegrityViolation_ThrowsConflict() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword(password);

        when(userRepository.existsByEmail(email.toLowerCase())).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("Duplicate"));

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.register(request));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("email already registered", exception.getReason());
    }

    @Test
    void register_EmailNormalized() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("  TEST@EXAMPLE.COM  ");
        request.setPassword(password);

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(registrationRepository.save(any(Registration.class))).thenReturn(registration);

        // When
        authService.register(request);

        // Then
        verify(userRepository).existsByEmail("test@example.com");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("test@example.com", userCaptor.getValue().getEmail());
    }

    @Test
    void register_DefaultRegistrationType() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword(password);

        when(userRepository.existsByEmail(email.toLowerCase())).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(registrationRepository.save(any(Registration.class))).thenReturn(registration);

        // When
        authService.register(request);

        // Then
        ArgumentCaptor<Registration> regCaptor = ArgumentCaptor.forClass(Registration.class);
        verify(registrationRepository).save(regCaptor.capture());
        assertEquals("STANDARD", regCaptor.getValue().getRegistrationType());
    }

    @Test
    void login_Success() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        String token = "jwt-token";

        when(userRepository.findByEmail(email.toLowerCase())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, user.getPasswordHash())).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.createAccessToken(user)).thenReturn(token);
        when(jwtService.createRefreshTokenValue()).thenReturn("refresh-token-value");
        when(jwtService.getRefreshTokenTtlSeconds()).thenReturn(604800L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        // When
        AuthResponse response = authService.login(request);

        // Then
        assertNotNull(response);
        assertEquals(token, response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertNotNull(response.getUser());
        assertEquals(userId, response.getUser().getId());
        verify(userRepository).findByEmail(email.toLowerCase());
        verify(passwordEncoder).matches(password, user.getPasswordHash());
        verify(userRepository).save(any(User.class));
        verify(jwtService).createAccessToken(user);
        verify(auditService).record(userId, AuditAction.LOGIN);
    }

    @Test
    void login_UserNotFound_ThrowsUnauthorized() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);

        when(userRepository.findByEmail(email.toLowerCase())).thenReturn(Optional.empty());

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login(request));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("invalid credentials", exception.getReason());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_WrongPassword_ThrowsUnauthorized() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword("wrongPassword");

        when(userRepository.findByEmail(email.toLowerCase())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", user.getPasswordHash())).thenReturn(false);

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login(request));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("invalid credentials", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
        verify(jwtService, never()).createAccessToken(any(User.class));
    }

    @Test
    void login_UpdatesLastLoginDate() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        String token = "jwt-token";

        when(userRepository.findByEmail(email.toLowerCase())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, user.getPasswordHash())).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.createAccessToken(user)).thenReturn(token);
        when(jwtService.createRefreshTokenValue()).thenReturn("refresh-token-value");
        when(jwtService.getRefreshTokenTtlSeconds()).thenReturn(604800L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        // When
        authService.login(request);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertNotNull(userCaptor.getValue().getLastLoginDate());
    }

    @Test
    void login_EmailNormalized() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("  TEST@EXAMPLE.COM  ");
        request.setPassword(password);
        String token = "jwt-token";

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, user.getPasswordHash())).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.createAccessToken(user)).thenReturn(token);
        when(jwtService.createRefreshTokenValue()).thenReturn("refresh-token-value");
        when(jwtService.getRefreshTokenTtlSeconds()).thenReturn(604800L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        // When
        authService.login(request);

        // Then
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void resendVerificationEmail_WhenVerificationDisabled_NoLookup() {
        when(userVerificationProperties.isRequireEmailVerification()).thenReturn(false);
        authService.resendVerificationEmail(email);
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void resendVerificationEmail_WhenUnknownEmail_NoSave() {
        when(userVerificationProperties.isRequireEmailVerification()).thenReturn(true);
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        authService.resendVerificationEmail(email);
        verify(userRepository, never()).save(any());
        verify(auditService, never()).record(any(), any());
    }

    @Test
    void resendVerificationEmail_WhenAlreadyVerified_NoSave() {
        when(userVerificationProperties.isRequireEmailVerification()).thenReturn(true);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        authService.resendVerificationEmail(email);
        verify(userRepository, never()).save(any());
        verify(auditService, never()).record(any(), any());
    }

    @Test
    void resendVerificationEmail_UnverifiedUser_NewTokenMailAndAudit() throws Exception {
        User unverified = new User(userId, email, "hash", Instant.now());
        unverified.setEmailVerificationToken("old-token");
        when(userVerificationProperties.isRequireEmailVerification()).thenReturn(true);
        when(userVerificationProperties.getVerificationTokenTtlSeconds()).thenReturn(86400);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(unverified));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(emailProperties.isEnabled()).thenReturn(true);
        when(emailProperties.getAppBaseUrl()).thenReturn("http://localhost:8080/checkin");
        when(emailProperties.getFromAddress()).thenReturn("noreply@test.com");
        when(emailProperties.getSubjectPrefix()).thenReturn("[T] ");
        when(userVerificationTemplate.render(anyString(), anyString())).thenReturn("<html/>");
        MimeMessage mime = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mime);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        authService.resendVerificationEmail(email);

        verify(userRepository).save(argThat(u ->
                u.getEmailVerificationToken() != null && !"old-token".equals(u.getEmailVerificationToken())));
        verify(mailSender).send(mime);
        verify(auditService).record(eq(userId), eq(AuditAction.RESEND_VERIFICATION_EMAIL));
    }
}
