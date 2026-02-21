package com.checkin.service;

import com.checkin.audit.AuditAction;
import com.checkin.model.AuditEvent;
import com.checkin.repository.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private AuditService auditService;

    private UUID userId;
    private AuditAction action;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        action = AuditAction.LOGIN;
    }

    @Test
    void record_WithDetails_Success() {
        // Given
        String details = "User logged in successfully";
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        auditService.record(userId, action, details);

        // Then
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent event = captor.getValue();
        assertEquals(userId, event.getUserId());
        assertEquals(action.name(), event.getAction());
        assertEquals(details, event.getDetails());
        assertNotNull(event.getCreatedAt());
    }

    @Test
    void record_WithoutDetails_Success() {
        // Given
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        auditService.record(userId, action);

        // Then
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent event = captor.getValue();
        assertEquals(userId, event.getUserId());
        assertEquals(action.name(), event.getAction());
        assertNull(event.getDetails());
        assertNotNull(event.getCreatedAt());
    }

    @Test
    void record_GeneratesUniqueId() {
        // Given
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        auditService.record(userId, action);

        // Then
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent event = captor.getValue();
        assertNotNull(event.getId());
    }

    @Test
    void record_SetsTimestamp() {
        // Given
        Instant before = Instant.now();
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        auditService.record(userId, action);

        // Then
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent event = captor.getValue();
        assertNotNull(event.getCreatedAt());
        assertTrue(event.getCreatedAt().isAfter(before.minusSeconds(1)));
        assertTrue(event.getCreatedAt().isBefore(Instant.now().plusSeconds(1)));
    }
}
