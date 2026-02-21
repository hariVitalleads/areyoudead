package com.checkin.service;

import com.checkin.config.AppMetrics;
import com.checkin.config.EmailProperties;
import com.checkin.config.EmergencyContactLimitProperties;
import com.checkin.config.EmergencyContactProperties;
import com.checkin.dto.EmergencyContactRequest;
import com.checkin.dto.EmergencyContactResponse;
import com.checkin.model.EmergencyContact;
import com.checkin.model.User;
import com.checkin.repository.EmergencyContactRepository;
import com.checkin.repository.RegistrationRepository;
import com.checkin.repository.UserRepository;
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
    private final EmergencyContactRepository emergencyContactRepository;
    private final RegistrationRepository registrationRepository;
    private final EmergencyContactProperties smsProperties;
    private final EmergencyContactLimitProperties limitProperties;
    private final EmailProperties emailProperties;
    private final JavaMailSender mailSender;
    private final InactiveUserEmailTemplate inactiveUserEmailTemplate;
    private final EmergencyContactVerificationTemplate verificationTemplate;
    private final AppMetrics metrics;
    private final UserRepository userRepository;

    public EmergencyContactService(
            EmergencyContactRepository emergencyContactRepository,
            RegistrationRepository registrationRepository,
            UserRepository userRepository,
            EmergencyContactProperties smsProperties,
            EmergencyContactLimitProperties limitProperties,
            EmailProperties emailProperties,
            JavaMailSender mailSender,
            InactiveUserEmailTemplate inactiveUserEmailTemplate,
            EmergencyContactVerificationTemplate verificationTemplate,
            AppMetrics metrics) {
        this.emergencyContactRepository = emergencyContactRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.smsProperties = smsProperties;
        this.limitProperties = limitProperties;
        this.emailProperties = emailProperties;
        this.mailSender = mailSender;
        this.inactiveUserEmailTemplate = inactiveUserEmailTemplate;
        this.verificationTemplate = verificationTemplate;
        this.metrics = metrics;
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

        int maxCount = limitProperties.getMaxCount();
        List<EmergencyContact> existing = emergencyContactRepository.findByUserIdOrderByContactIndexAsc(userId);
        if (existing.size() >= maxCount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "max emergency counts reached");
        }

        short nextIndex = (short) (existing.isEmpty() ? 1 : existing.get(existing.size() - 1).getContactIndex() + 1);

        EmergencyContact contact = new EmergencyContact(
                UUID.randomUUID(),
                userId,
                nextIndex,
                req.getMobileNumber(),
                req.getEmail(),
                Instant.now());
        contact.setOptOutToken(UUID.randomUUID());
        String verificationToken = UUID.randomUUID().toString();
        contact.setVerificationToken(verificationToken);
        contact.setVerificationTokenExpiresAt(Instant.now().plusSeconds(limitProperties.getVerificationTokenTtlSeconds()));
        if (req.getLabel() != null && !req.getLabel().isBlank()) {
            contact.setLabel(req.getLabel().trim());
        }

        EmergencyContact saved = emergencyContactRepository.save(contact);
        sendVerificationEmail(saved);
        return toResponse(saved);
    }

    private void sendVerificationEmail(EmergencyContact contact) {
        if (!emailProperties.isEnabled()) {
            log.debug("Email disabled, skipping verification email for contact: {}", contact.getId());
            return;
        }
        String baseUrl = emailProperties.getAppBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            log.warn("APP_BASE_URL not set, cannot send verification email to contact: {}", contact.getId());
            return;
        }
        String userEmail = userRepository.findById(contact.getUserId())
                .map(User::getEmail)
                .orElse("a Checkin user");
        String verifyUrl = baseUrl.replaceAll("/$", "") + "/api/emergency-contacts/verify/" + contact.getVerificationToken();
        String htmlBody = verificationTemplate.render(userEmail, verifyUrl);
        try {
            sendHtmlEmail(contact.getEmail(), "Verify your email", htmlBody);
            log.info("Sent verification email to contact: {} ({})", contact.getId(), contact.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email to contact: {}", contact.getId(), e);
        }
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
        if (req.getLabel() != null) {
            contact.setLabel(req.getLabel().isBlank() ? null : req.getLabel().trim());
        }

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

        List<EmergencyContact> contacts = filterContactsForAlerts(
                emergencyContactRepository.findByUserIdOrderByContactIndexAsc(userId));
        if (contacts.isEmpty()) {
            log.debug("No emergency contacts found for user: {} (or filtered out)", userId);
            return;
        }

        log.info("Sending SMS to {} emergency contact(s) for user: {}", contacts.size(), userId);
        for (EmergencyContact contact : contacts) {
            try {
                sendSms(contact.getMobileNumber(), message);
                metrics.recordAlertSmsSent();
                log.debug("SMS sent successfully to emergency contact: {} (mobile: {})",
                        contact.getId(), contact.getMobileNumber());
            } catch (Exception e) {
                metrics.recordAlertSmsFailed();
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

        List<EmergencyContact> contacts = filterContactsForAlerts(
                emergencyContactRepository.findByUserIdOrderByContactIndexAsc(userId));
        if (contacts.isEmpty()) {
            log.debug("No emergency contacts found for user: {} (or filtered out)", userId);
            return;
        }

        log.info("Sending email to {} emergency contact(s) for user: {}", contacts.size(), userId);
        for (EmergencyContact contact : contacts) {
            try {
                sendEmail(contact.getEmail(), subject, body);
                metrics.recordAlertEmailSent();
                log.debug("Email sent successfully to emergency contact: {} (email: {})",
                        contact.getId(), contact.getEmail());
            } catch (Exception e) {
                metrics.recordAlertEmailFailed();
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

        List<EmergencyContact> contacts = filterContactsForAlerts(
                emergencyContactRepository.findByUserIdOrderByContactIndexAsc(user.getId()));
        if (contacts.isEmpty()) {
            log.debug("No emergency contacts found for user: {} (or all opted out)", user.getId());
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
        String baseUrl = emailProperties.getAppBaseUrl();
        for (EmergencyContact contact : contacts) {
            try {
                String htmlWithOptOut = appendOptOutLink(htmlBody, baseUrl, contact.getOptOutToken());
                sendHtmlEmail(contact.getEmail(), subject, htmlWithOptOut != null ? htmlWithOptOut : htmlBody);
                metrics.recordAlertEmailSent();
                log.debug("Inactive user alert email sent to emergency contact: {} (email: {})",
                        contact.getId(), contact.getEmail());
            } catch (Exception e) {
                metrics.recordAlertEmailFailed();
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

    private String appendOptOutLink(String htmlBody, String baseUrl, java.util.UUID optOutToken) {
        if (baseUrl == null || baseUrl.isBlank() || optOutToken == null) {
            return htmlBody;
        }
        String optOutUrl = baseUrl.replaceAll("/$", "") + "/api/emergency-contacts/opt-out/" + optOutToken;
        String linkBlock = "<p style=\"margin-top:16px;font-size:12px;color:#666;\">"
                + "If you do not wish to receive these alerts, <a href=\"" + optOutUrl + "\">opt out here</a>.</p>";
        if (htmlBody.contains("</body>")) {
            return htmlBody.replace("</body>", linkBlock + "</body>");
        }
        return htmlBody + linkBlock;
    }

    private List<EmergencyContact> filterContactsForAlerts(List<EmergencyContact> contacts) {
        return contacts.stream()
                .filter(c -> c.getOptedOutAt() == null)
                .filter(c -> !limitProperties.isRequireVerification() || c.getVerifiedAt() != null)
                .collect(Collectors.toList());
    }

    /**
     * Verify an emergency contact's email. Called when contact clicks verification link.
     */
    @Transactional
    public void verifyByToken(String verificationToken) {
        EmergencyContact contact = emergencyContactRepository.findByVerificationToken(verificationToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "invalid or expired verification link"));
        if (contact.getVerifiedAt() != null) {
            throw new ResponseStatusException(HttpStatus.GONE, "already verified");
        }
        if (contact.getVerificationTokenExpiresAt() != null
                && contact.getVerificationTokenExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "verification link expired");
        }
        contact.setVerifiedAt(Instant.now());
        contact.setVerificationToken(null);
        contact.setVerificationTokenExpiresAt(null);
        emergencyContactRepository.save(contact);
        log.info("Emergency contact {} verified email", contact.getId());
    }

    /**
     * Opt out an emergency contact from future alerts. Called when contact clicks opt-out link.
     */
    @Transactional
    public void optOutByToken(UUID optOutToken) {
        EmergencyContact contact = emergencyContactRepository.findByOptOutToken(optOutToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "invalid or expired opt-out link"));
        if (contact.getOptedOutAt() != null) {
            throw new ResponseStatusException(HttpStatus.GONE, "already opted out");
        }
        contact.setOptedOutAt(Instant.now());
        emergencyContactRepository.save(contact);
        log.info("Emergency contact {} opted out from alerts", contact.getId());
    }

    private EmergencyContactResponse toResponse(EmergencyContact contact) {
        return new EmergencyContactResponse(
                contact.getId(),
                contact.getMobileNumber(),
                contact.getEmail(),
                contact.getContactIndex(),
                contact.getVerifiedAt() != null,
                contact.getLabel());
    }

    /**
     * Sends SMS to the first maxContacts emergency contacts (escalation support).
     */
    public void sendSmsToContactsUpTo(UUID userId, String message, int maxContacts) {
        if (!smsProperties.isEnabled()) return;
        List<EmergencyContact> all = filterContactsForAlerts(
                emergencyContactRepository.findByUserIdOrderByContactIndexAsc(userId));
        List<EmergencyContact> toNotify = all.stream().limit(maxContacts).collect(Collectors.toList());
        if (toNotify.isEmpty()) return;
        log.info("Sending SMS to {} emergency contact(s) for user: {} (escalation level {})", toNotify.size(), userId, maxContacts);
        for (EmergencyContact contact : toNotify) {
            try {
                sendSms(contact.getMobileNumber(), message);
                metrics.recordAlertSmsSent();
            } catch (Exception e) {
                metrics.recordAlertSmsFailed();
                log.error("Failed to send SMS to emergency contact: {}", contact.getId(), e);
            }
        }
    }

    /**
     * Sends inactive user alert email to the first maxContacts emergency contacts (escalation support).
     */
    public void sendInactiveUserAlertToContactsUpTo(User user, long inactiveMs, int maxContacts) {
        if (!emailProperties.isEnabled()) return;
        List<EmergencyContact> all = filterContactsForAlerts(
                emergencyContactRepository.findByUserIdOrderByContactIndexAsc(user.getId()));
        List<EmergencyContact> toNotify = all.stream().limit(maxContacts).collect(Collectors.toList());
        if (toNotify.isEmpty()) return;
        String subject = "Inactivity Alert";
        String htmlBody = inactiveUserEmailTemplate.render(
                user.getEmail(), user.getId().toString(), user.getLastLoginDate(), inactiveMs);
        String baseUrl = emailProperties.getAppBaseUrl();
        log.info("Sending inactive user alert to {} emergency contact(s) for user: {} (escalation level {})",
                toNotify.size(), user.getId(), maxContacts);
        for (EmergencyContact contact : toNotify) {
            try {
                String htmlWithOptOut = appendOptOutLink(htmlBody, baseUrl, contact.getOptOutToken());
                sendHtmlEmail(contact.getEmail(), subject, htmlWithOptOut != null ? htmlWithOptOut : htmlBody);
                metrics.recordAlertEmailSent();
            } catch (Exception e) {
                metrics.recordAlertEmailFailed();
                log.error("Failed to send inactive user alert to emergency contact: {}", contact.getId(), e);
            }
        }
    }
}
