package com.areyoudead.service;

import com.areyoudead.config.EmailProperties;
import com.areyoudead.config.EmergencyContactProperties;
import com.areyoudead.dto.EmergencyContactRequest;
import com.areyoudead.dto.EmergencyContactResponse;
import com.areyoudead.model.EmergencyContact;
import com.areyoudead.repository.EmergencyContactRepository;
import com.areyoudead.repository.RegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Properties;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmergencyContactServiceTest {

    @Mock
    private EmergencyContactRepository emergencyContactRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private EmergencyContactProperties smsProperties;

    @Mock
    private EmailProperties emailProperties;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private InactiveUserEmailTemplate inactiveUserEmailTemplate;

    @InjectMocks
    private EmergencyContactService emergencyContactService;

    private UUID userId;
    private UUID contactId;
    private EmergencyContact contact;
    private EmergencyContactRequest request;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        contactId = UUID.randomUUID();
        contact = new EmergencyContact(
                contactId,
                userId,
                (short) 0,
                "+1234567890",
                "contact@example.com",
                Instant.now());
        request = new EmergencyContactRequest();
        request.setMobileNumber("+1234567890");
        request.setEmail("contact@example.com");
    }

    @Test
    void getContacts_Success() {
        // Given
        List<EmergencyContact> contacts = List.of(contact);
        when(emergencyContactRepository.findByUserIdOrderByContactIndexAsc(userId)).thenReturn(contacts);

        // When
        List<EmergencyContactResponse> responses = emergencyContactService.getContacts(userId);

        // Then
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(contactId, responses.get(0).getId());
        assertEquals("+1234567890", responses.get(0).getMobileNumber());
        assertEquals("contact@example.com", responses.get(0).getEmail());
    }

    @Test
    void getContacts_EmptyList() {
        // Given
        when(emergencyContactRepository.findByUserIdOrderByContactIndexAsc(userId)).thenReturn(new ArrayList<>());

        // When
        List<EmergencyContactResponse> responses = emergencyContactService.getContacts(userId);

        // Then
        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void addContact_Success() {
        // Given
        when(registrationRepository.existsById(userId)).thenReturn(true);
        when(emergencyContactRepository.findByUserIdOrderByContactIndexAsc(userId)).thenReturn(new ArrayList<>());
        when(emergencyContactRepository.save(any(EmergencyContact.class))).thenReturn(contact);

        // When
        EmergencyContactResponse response = emergencyContactService.addContact(userId, request);

        // Then
        assertNotNull(response);
        assertEquals(contactId, response.getId());
        verify(registrationRepository).existsById(userId);
        verify(emergencyContactRepository).save(any(EmergencyContact.class));
    }

    @Test
    void addContact_RegistrationNotFound_ThrowsNotFound() {
        // Given
        when(registrationRepository.existsById(userId)).thenReturn(false);

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> emergencyContactService.addContact(userId, request));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("registration not found", exception.getReason());
        verify(emergencyContactRepository, never()).save(any(EmergencyContact.class));
    }

    @Test
    void addContact_MaxContactsReached_ThrowsBadRequest() {
        // Given
        List<EmergencyContact> existing = List.of(
                createContact((short) 0),
                createContact((short) 1),
                createContact((short) 2));
        when(registrationRepository.existsById(userId)).thenReturn(true);
        when(emergencyContactRepository.findByUserIdOrderByContactIndexAsc(userId)).thenReturn(existing);

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> emergencyContactService.addContact(userId, request));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("maximum of 3 contacts allowed"));
        verify(emergencyContactRepository, never()).save(any(EmergencyContact.class));
    }

    @Test
    void addContact_SetsCorrectIndex() {
        // Given: one existing contact with index 1, next should be 2
        List<EmergencyContact> existing = List.of(createContact((short) 1));
        when(registrationRepository.existsById(userId)).thenReturn(true);
        when(emergencyContactRepository.findByUserIdOrderByContactIndexAsc(userId)).thenReturn(existing);
        when(emergencyContactRepository.save(any(EmergencyContact.class))).thenReturn(contact);

        // When
        emergencyContactService.addContact(userId, request);

        // Then
        ArgumentCaptor<EmergencyContact> captor = ArgumentCaptor.forClass(EmergencyContact.class);
        verify(emergencyContactRepository).save(captor.capture());
        assertEquals((short) 2, captor.getValue().getContactIndex());
    }

    @Test
    void updateContact_Success() {
        // Given
        EmergencyContactRequest updateRequest = new EmergencyContactRequest();
        updateRequest.setMobileNumber("+9876543210");
        updateRequest.setEmail("updated@example.com");

        when(emergencyContactRepository.findById(contactId)).thenReturn(Optional.of(contact));
        when(emergencyContactRepository.save(any(EmergencyContact.class))).thenReturn(contact);

        // When
        EmergencyContactResponse response = emergencyContactService.updateContact(userId, contactId, updateRequest);

        // Then
        assertNotNull(response);
        ArgumentCaptor<EmergencyContact> captor = ArgumentCaptor.forClass(EmergencyContact.class);
        verify(emergencyContactRepository).save(captor.capture());
        assertEquals("+9876543210", captor.getValue().getMobileNumber());
        assertEquals("updated@example.com", captor.getValue().getEmail());
    }

    @Test
    void updateContact_ContactNotFound_ThrowsNotFound() {
        // Given
        when(emergencyContactRepository.findById(contactId)).thenReturn(Optional.empty());

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> emergencyContactService.updateContact(userId, contactId, request));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("contact not found", exception.getReason());
    }

    @Test
    void updateContact_DifferentUserId_ThrowsForbidden() {
        // Given
        UUID differentUserId = UUID.randomUUID();
        when(emergencyContactRepository.findById(contactId)).thenReturn(Optional.of(contact));

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> emergencyContactService.updateContact(differentUserId, contactId, request));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("access denied", exception.getReason());
        verify(emergencyContactRepository, never()).save(any(EmergencyContact.class));
    }

    @Test
    void deleteContact_Success() {
        // Given
        when(emergencyContactRepository.findById(contactId)).thenReturn(Optional.of(contact));

        // When
        emergencyContactService.deleteContact(userId, contactId);

        // Then
        verify(emergencyContactRepository).delete(contact);
    }

    @Test
    void deleteContact_ContactNotFound_ThrowsNotFound() {
        // Given
        when(emergencyContactRepository.findById(contactId)).thenReturn(Optional.empty());

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> emergencyContactService.deleteContact(userId, contactId));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("contact not found", exception.getReason());
        verify(emergencyContactRepository, never()).delete(any(EmergencyContact.class));
    }

    @Test
    void deleteContact_DifferentUserId_ThrowsForbidden() {
        // Given
        UUID differentUserId = UUID.randomUUID();
        when(emergencyContactRepository.findById(contactId)).thenReturn(Optional.of(contact));

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> emergencyContactService.deleteContact(differentUserId, contactId));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("access denied", exception.getReason());
        verify(emergencyContactRepository, never()).delete(any(EmergencyContact.class));
    }

    // -------------------------------------------------------------------------
    // SMS tests
    // -------------------------------------------------------------------------

    @Test
    void sendSmsToAllContacts_SmsEnabled_SendsSms() {
        // Given
        String message = "Test message";
        List<EmergencyContact> contacts = List.of(contact);
        when(smsProperties.isEnabled()).thenReturn(true);
        when(emergencyContactRepository.findByUserIdOrderByContactIndexAsc(userId)).thenReturn(contacts);

        // When
        emergencyContactService.sendSmsToAllContacts(userId, message);

        // Then
        verify(emergencyContactRepository).findByUserIdOrderByContactIndexAsc(userId);
        // Note: sendSms is private, so we verify indirectly through repository call
    }

    @Test
    void sendSmsToAllContacts_SmsDisabled_SkipsSms() {
        // Given
        String message = "Test message";
        when(smsProperties.isEnabled()).thenReturn(false);

        // When
        emergencyContactService.sendSmsToAllContacts(userId, message);

        // Then
        verify(emergencyContactRepository, never()).findByUserIdOrderByContactIndexAsc(any());
    }

    @Test
    void sendSmsToAllContacts_NoContacts_SkipsSms() {
        // Given
        String message = "Test message";
        when(smsProperties.isEnabled()).thenReturn(true);
        when(emergencyContactRepository.findByUserIdOrderByContactIndexAsc(userId)).thenReturn(new ArrayList<>());

        // When
        emergencyContactService.sendSmsToAllContacts(userId, message);

        // Then
        verify(emergencyContactRepository).findByUserIdOrderByContactIndexAsc(userId);
    }

    // -------------------------------------------------------------------------
    // Email tests
    // -------------------------------------------------------------------------

    @Test
    void sendEmailToAllContacts_EmailEnabled_SendsEmail() throws Exception {
        // Given
        String subject = "Inactivity Alert";
        String body = "User has been inactive.";
        List<EmergencyContact> contacts = List.of(contact);
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(emailProperties.isEnabled()).thenReturn(true);
        when(emailProperties.getFromAddress()).thenReturn("noreply@areyoudead.com");
        when(emailProperties.getSubjectPrefix()).thenReturn("[AreYouAlive]");
        when(emergencyContactRepository.findByUserIdOrderByContactIndexAsc(userId)).thenReturn(contacts);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        emergencyContactService.sendEmailToAllContacts(userId, subject, body);

        // Then
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendEmailToAllContacts_EmailDisabled_SkipsEmail() {
        // Given
        when(emailProperties.isEnabled()).thenReturn(false);

        // When
        emergencyContactService.sendEmailToAllContacts(userId, "Alert", "body");

        // Then
        verify(emergencyContactRepository, never()).findByUserIdOrderByContactIndexAsc(any());
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendEmailToAllContacts_NoContacts_SkipsEmail() {
        // Given
        when(emailProperties.isEnabled()).thenReturn(true);
        when(emergencyContactRepository.findByUserIdOrderByContactIndexAsc(userId)).thenReturn(new ArrayList<>());

        // When
        emergencyContactService.sendEmailToAllContacts(userId, "Alert", "body");

        // Then
        verify(emergencyContactRepository).findByUserIdOrderByContactIndexAsc(userId);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    private EmergencyContact createContact(short index) {
        return new EmergencyContact(
                UUID.randomUUID(),
                userId,
                index,
                "+1234567890",
                "contact" + index + "@example.com",
                Instant.now());
    }
}
