package com.areyoudead.service;

import com.areyoudead.dto.EmergencyContactRequest;
import com.areyoudead.dto.EmergencyContactResponse;
import com.areyoudead.model.EmergencyContact;
import com.areyoudead.repository.EmergencyContactRepository;
import com.areyoudead.repository.RegistrationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmergencyContactService {
    private static final int MAX_CONTACTS = 3;
    private final EmergencyContactRepository emergencyContactRepository;
    private final RegistrationRepository registrationRepository;

    public EmergencyContactService(
            EmergencyContactRepository emergencyContactRepository,
            RegistrationRepository registrationRepository) {
        this.emergencyContactRepository = emergencyContactRepository;
        this.registrationRepository = registrationRepository;
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

    private EmergencyContactResponse toResponse(EmergencyContact contact) {
        return new EmergencyContactResponse(
                contact.getId(),
                contact.getMobileNumber(),
                contact.getEmail(),
                contact.getContactIndex());
    }
}
