package com.areyoudead.service;

import com.areyoudead.config.EmergencyContactProperties;
import com.areyoudead.dto.EmergencyContactRequest;
import com.areyoudead.dto.EmergencyContactResponse;
import com.areyoudead.model.EmergencyContact;
import com.areyoudead.repository.EmergencyContactRepository;
import com.areyoudead.repository.RegistrationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmergencyContactService {
    private static final Logger log = LoggerFactory.getLogger(EmergencyContactService.class);
    private static final int MAX_CONTACTS = 3;
    private final EmergencyContactRepository emergencyContactRepository;
    private final RegistrationRepository registrationRepository;
    private final EmergencyContactProperties smsProperties;

    public EmergencyContactService(
            EmergencyContactRepository emergencyContactRepository,
            RegistrationRepository registrationRepository,
            EmergencyContactProperties smsProperties) {
        this.emergencyContactRepository = emergencyContactRepository;
        this.registrationRepository = registrationRepository;
        this.smsProperties = smsProperties;
    }

    public List<EmergencyContactResponse> getContacts(UUID userId) {
        return emergencyContactRepository.findByUserIdOrderByContactIndexAsc(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public EmergencyContactResponse addContact(UUID userId, EmergencyContactRequest req) {
        if (!registrationRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "registration not found");
        }

        List<EmergencyContact> existing = emergencyContactRepository.findByUserIdOrderByContactIndexAsc(userId);
        if (existing.size() >= MAX_CONTACTS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "maximum of " + MAX_CONTACTS + " contacts allowed");
        }

        short nextIndex = (short) (existing.isEmpty() ? 0 : existing.get(existing.size() - 1).getContactIndex() + 1);

        EmergencyContact contact = new EmergencyContact(
                UUID.randomUUID(),
                userId,
                nextIndex,
                req.getMobileNumber(),
                req.getEmail(),
                Instant.now());

        return toResponse(emergencyContactRepository.save(contact));
    }

    @Transactional
    public EmergencyContactResponse updateContact(UUID userId, UUID contactId, EmergencyContactRequest req) {
        EmergencyContact contact = emergencyContactRepository.findById(contactId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "contact not found"));

        if (!contact.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "access denied");
        }

        contact.setMobileNumber(req.getMobileNumber());
        contact.setEmail(req.getEmail());

        return toResponse(emergencyContactRepository.save(contact));
    }

    @Transactional
    public void deleteContact(UUID userId, UUID contactId) {
        EmergencyContact contact = emergencyContactRepository.findById(contactId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "contact not found"));

        if (!contact.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "access denied");
        }

        emergencyContactRepository.delete(contact);
    }

    /**
     * Sends SMS to all emergency contacts for a given user.
     * Only sends if SMS is enabled via configuration.
     *
     * @param userId The user ID whose emergency contacts should be notified
     * @param message The message to send
     */
    public void sendSmsToAllContacts(UUID userId, String message) {
        if (!smsProperties.isEnabled()) {
            log.debug("SMS to emergency contacts is disabled. Skipping SMS for user: {}", userId);
            return;
        }

        List<EmergencyContact> contacts = emergencyContactRepository.findByUserIdOrderByContactIndexAsc(userId);
        if (contacts.isEmpty()) {
            log.debug("No emergency contacts found for user: {}", userId);
            return;
        }

        log.info("Sending SMS to {} emergency contact(s) for user: {}", contacts.size(), userId);
        for (EmergencyContact contact : contacts) {
            try {
                sendSms(contact.getMobileNumber(), message);
                log.debug("SMS sent successfully to emergency contact: {} (mobile: {})", 
                        contact.getId(), contact.getMobileNumber());
            } catch (Exception e) {
                log.error("Failed to send SMS to emergency contact: {} (mobile: {})", 
                        contact.getId(), contact.getMobileNumber(), e);
            }
        }
    }

    /**
     * Sends an SMS message to the specified mobile number.
     * This is a placeholder implementation - replace with actual SMS provider integration.
     *
     * @param mobileNumber The mobile number to send SMS to
     * @param message The message content
     */
    private void sendSms(String mobileNumber, String message) {
        // TODO: Integrate with actual SMS provider (e.g., Twilio, AWS SNS, etc.)
        log.info("SMS to {}: {}", mobileNumber, message);
    }

    private EmergencyContactResponse toResponse(EmergencyContact contact) {
        return new EmergencyContactResponse(
                contact.getId(),
                contact.getMobileNumber(),
                contact.getEmail(),
                contact.getContactIndex());
    }
}
