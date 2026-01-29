package com.areyoudead.scheduler;

import com.areyoudead.model.User;
import com.areyoudead.repository.UserRepository;
import com.areyoudead.service.EmergencyContactService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InactiveUserScheduler {
    private static final Logger log = LoggerFactory.getLogger(InactiveUserScheduler.class);

    private final UserRepository userRepository;
    private final EmergencyContactService emergencyContactService;
    private final int inactiveDays;

    public InactiveUserScheduler(
            UserRepository userRepository,
            EmergencyContactService emergencyContactService,
            @Value("${app.scheduler.inactive-days:3}") int inactiveDays) {
        this.userRepository = userRepository;
        this.emergencyContactService = emergencyContactService;
        this.inactiveDays = inactiveDays;
    }

    @Scheduled(cron = "0 0 0 * * *") // Runs daily at midnight
    public void checkInactiveUsers() {
        log.info("Starting daily check for inactive users (threshold: {} days)", inactiveDays);

        Instant cutoffDate = Instant.now().minus(inactiveDays, ChronoUnit.DAYS);
        List<User> inactiveUsers = userRepository.findByLastLoginDateBefore(cutoffDate);

        if (inactiveUsers.isEmpty()) {
            log.info("No inactive users found.");
            return;
        }

        log.info("Found {} inactive users.", inactiveUsers.size());
        for (User user : inactiveUsers) {
            log.info("Inactive User Detected: ID={}, Email={}, LastLogin={}",
                    user.getId(), user.getEmail(), user.getLastLoginDate());
            
            String message = String.format(
                    "Alert: User %s (%s) has been inactive for %d days. Last login: %s",
                    user.getEmail(), user.getId(), inactiveDays, user.getLastLoginDate());
            emergencyContactService.sendSmsToAllContacts(user.getId(), message);
        }
    }
}
