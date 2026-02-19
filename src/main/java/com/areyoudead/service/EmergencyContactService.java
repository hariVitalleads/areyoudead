package com.areyoudead.service;

import com.areyoudead.config.EmailProperties;
import com.areyoudead.config.EmergencyContactProperties;
import com.areyoudead.dto.EmergencyContactRequest;
import com.areyoudead.dto.EmergencyContactResponse;
import com.areyoudead.model.EmergencyContact;
import com.areyoudead.model.User;
import com.areyoudead.repository.EmergencyContactRepository;
import com.areyoudead.repository.RegistrationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
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
    private final EmailProperties emailProperties;
    private final JavaMailSender mailSender;
    private final InactiveUserEmailTemplate inactiveUserEmailTemplate;

    public EmergencyContactService(
            EmergencyContactRepository emergencyContactRepository,
            RegistrationRepository registrationRepository,
            EmergencyContactProperties smsProperties,
            EmailProperties emailProperties,
            JavaMailSender mailSender,
            InactiveUserEmailTemplate inactiveUserEmailTemplate) {
        this.emergencyContactRepository = emergencyContactRepository;
        this.registrationRepository = registrationRepository;
        this.smsProperties = smsProperties;
        this.emailProperties = emailProperties;
        this.mailSender = mailSender;
        this.inactiveUserEmailTemplate = inactiveUserEmailTemplate;
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

        short nextIndex = (short) (existing.isEmpty() ? 1 : existing.get(existing.size() - 1).getContactIndex() + 1);

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
     * @param userId  the user ID whose emergency contacts should be notified
     * @param message the message to send
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
     * Sends an email to all emergency contacts for a given user.
     * Only sends if email is enabled via configuration.
     *
     * @param userId  the user ID whose emergency contacts should be notified
     * @param subject the email subject (the configured prefix is prepended
     *                automatically)
     * @param body    the plain-text email body
     */
    public void sendEmailToAllContacts(UUID userId, String subject, String body) {
        if (!emailProperties.isEnabled()) {
            log.debug("Email to emergency contacts is disabled. Skipping email for user: {}", userId);
            return;
        }

        List<EmergencyContact> contacts = emergencyContactRepository.findByUserIdOrderByContactIndexAsc(userId);
        if (contacts.isEmpty()) {
            log.debug("No emergency contacts found for user: {}", userId);
            return;
        }

        log.info("Sending email to {} emergency contact(s) for user: {}", contacts.size(), userId);
        for (EmergencyContact contact : contacts) {
            try {
                sendEmail(contact.getEmail(), subject, body);
                log.debug("Email sent successfully to emergency contact: {} (email: {})",
                        contact.getId(), contact.getEmail());
            } catch (Exception e) {
                log.error("Failed to send email to emergency contact: {} (email: {})",
                        contact.getId(), contact.getEmail(), e);
            }
        }
    }

    /**
     * Sends an HTML-templated inactive user alert to all emergency contacts.
     * Only sends if email is enabled via configuration.
     *
     * @param user       the inactive user
     * @param inactiveMs how long the user has been inactive (milliseconds)
     */
    public void sendInactiveUserAlertToContacts(User user, long inactiveMs) {
        if (!emailProperties.isEnabled()) {
            log.debug("Email to emergency contacts is disabled. Skipping inactive user alert for: {}", user.getId());
            return;
        }

        List<EmergencyContact> contacts = emergencyContactRepository.findByUserIdOrderByContactIndexAsc(user.getId());
        if (contacts.isEmpty()) {
            log.debug("No emergency contacts found for user: {}", user.getId());
            return;
        }

        String subject = "Inactivity Alert";
        String htmlBody = inactiveUserEmailTemplate.render(
                user.getEmail(),
                user.getId().toString(),
                user.getLastLoginDate(),
                inactiveMs);

        if (log.isDebugEnabled()) {
            log.debug("Generated inactive user alert email HTML (user={}, length={} chars):\n{}",
                    user.getId(), htmlBody.length(), htmlBody);
        }
        log.info("Sending inactive user alert email to {} emergency contact(s) for user: {}", contacts.size(), user.getId());
        for (EmergencyContact contact : contacts) {
            try {
                sendHtmlEmail(contact.getEmail(), subject, htmlBody);
                log.debug("Inactive user alert email sent to emergency contact: {} (email: {})",
                        contact.getId(), contact.getEmail());
            } catch (Exception e) {
                log.error("Failed to send inactive user alert to emergency contact: {} (email: {})",
                        contact.getId(), contact.getEmail(), e);
            }
        }
    }

    /**
     * Sends an SMS message to the specified mobile number.
     * This is a placeholder implementation — replace with actual SMS provider
     * integration.
     *
     * @param mobileNumber the mobile number to send SMS to
     * @param message      the message content
     */
    private void sendSms(String mobileNumber, String message) {
        // TODO: Integrate with actual SMS provider (e.g., Twilio, AWS SNS, etc.)
        log.info("SMS to {}: {}", mobileNumber, message);
    }

    /**
     * Sends a plain-text email to the specified address via {@link JavaMailSender}.
     *
     * @param to      recipient address
     * @param subject raw subject (the configured prefix is prepended)
     * @param body    plain-text body
     */
    private void sendEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(emailProperties.getFromAddress());
            helper.setTo(to);
            helper.setSubject(emailProperties.getSubjectPrefix() + " " + subject);
            helper.setText(body, false);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Sends an HTML email to the specified address.
     *
     * @param to      recipient address
     * @param subject raw subject (the configured prefix is prepended)
     * @param htmlBody HTML body
     */
    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(emailProperties.getFromAddress());
            helper.setTo(to);
            helper.setSubject(emailProperties.getSubjectPrefix() + " " + subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private EmergencyContactResponse toResponse(EmergencyContact contact) {
        return new EmergencyContactResponse(
                contact.getId(),
                contact.getMobileNumber(),
                contact.getEmail(),
                contact.getContactIndex());
    }
}
