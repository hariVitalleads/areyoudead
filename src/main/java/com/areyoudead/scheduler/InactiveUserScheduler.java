package com.areyoudead.scheduler;

import com.areyoudead.model.User;
import com.areyoudead.repository.UserRepository;
import com.areyoudead.service.EmergencyContactService;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class InactiveUserScheduler {
    private static final Logger log = LoggerFactory.getLogger(InactiveUserScheduler.class);

    private final UserRepository userRepository;
    private final EmergencyContactService emergencyContactService;
    private final long inactiveMs;
    private final boolean smsEnabled;
    private final boolean emailEnabled;

    public InactiveUserScheduler(
            UserRepository userRepository,
            EmergencyContactService emergencyContactService,
            @Value("${app.scheduler.inactive-ms:86400000}") long inactiveMs,
            @Value("${app.scheduler.emergency-contacts.sms.enabled:true}") boolean smsEnabled,
            @Value("${app.scheduler.emergency-contacts.email.enabled:true}") boolean emailEnabled) {
        this.userRepository = userRepository;
        this.emergencyContactService = emergencyContactService;
        this.inactiveMs = inactiveMs;
        this.smsEnabled = smsEnabled;
        this.emailEnabled = emailEnabled;
    }

    @Scheduled(fixedRate = 10000) // Every 10 seconds
    public void checkInactiveUsers() {
        log.info("Starting daily check for inactive users (threshold: {} ms)", inactiveMs);

        Instant cutoffDate = Instant.now().minusMillis(inactiveMs);
        log.info("Cutoff date: {}", cutoffDate);
        List<User> inactiveUsers = userRepository.findByLastLoginDateBefore(cutoffDate);
        Optional<User> user = userRepository.findByEmail("user_tmuara@example.com");
        user.ifPresent(u -> {
            log.info("User found: ID={}, Email={}, LastLogin={}", u.getId(), u.getEmail(), u.getLastLoginDate());

            if (emailEnabled) {
                emergencyContactService.sendInactiveUserAlertToContacts(user.get(), inactiveMs);
            }
        });
        /*
        if (inactiveUsers.isEmpty()) {
            log.info("No inactive users found.");
            return;
        }

        log.info("Found {} inactive users.", inactiveUsers.size());
        for (User user : inactiveUsers) {
            log.info("Inactive User Detected: ID={}, Email={}, LastLogin={}",
                    user.getId(), user.getEmail(), user.getLastLoginDate());

            if (smsEnabled) {
                String smsMessage = String.format(
                        "Alert: User %s (%s) has been inactive for %d ms. Last login: %s",
                        user.getEmail(), user.getId(), inactiveMs, user.getLastLoginDate());
                emergencyContactService.sendSmsToAllContacts(user.getId(), smsMessage);
            }
            if (emailEnabled) {
                emergencyContactService.sendInactiveUserAlertToContacts(user, inactiveMs);
            }
        }
             */
    }
}
