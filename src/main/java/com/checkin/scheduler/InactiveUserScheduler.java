package com.checkin.scheduler;

import com.checkin.config.AppMetrics;
import com.checkin.model.AlertChannelPreference;
import com.checkin.model.User;
import com.checkin.repository.EmergencyContactRepository;
import com.checkin.repository.UserRepository;
import com.checkin.service.EmergencyContactService;
import java.time.Instant;
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
    private final EmergencyContactRepository emergencyContactRepository;
    private final EmergencyContactService emergencyContactService;
    private final AppMetrics metrics;
    private final long defaultInactiveMs;
    private final long escalationIntervalMs;
    private final boolean smsEnabled;
    private final boolean emailEnabled;

    public InactiveUserScheduler(
            UserRepository userRepository,
            EmergencyContactRepository emergencyContactRepository,
            EmergencyContactService emergencyContactService,
            AppMetrics metrics,
            @Value("${app.scheduler.inactive-ms:86400000}") long defaultInactiveMs,
            @Value("${app.scheduler.escalation-interval-hours:24}") long escalationIntervalHours,
            @Value("${app.scheduler.emergency-contacts.sms.enabled:true}") boolean smsEnabled,
            @Value("${app.scheduler.emergency-contacts.email.enabled:true}") boolean emailEnabled) {
        this.userRepository = userRepository;
        this.emergencyContactRepository = emergencyContactRepository;
        this.emergencyContactService = emergencyContactService;
        this.metrics = metrics;
        this.defaultInactiveMs = defaultInactiveMs;
        this.escalationIntervalMs = escalationIntervalHours * 3600 * 1000;
        this.smsEnabled = smsEnabled;
        this.emailEnabled = emailEnabled;
    }

    @Scheduled(fixedRate = 10000) // Every 10 seconds
    public void checkInactiveUsers() {
        metrics.recordSchedulerRun();
        metrics.getSchedulerDurationTimer().record(() -> {
        try {
            Instant now = Instant.now();
            Instant minCutoff = now.minusMillis(Math.min(defaultInactiveMs, 86400000L)); // At least 1 day lookback
            List<User> candidates = userRepository.findByLastLoginDateBefore(minCutoff);

            List<User> inactiveUsers = candidates.stream()
                .filter(u -> u.getAlertsSnoozedUntil() == null || u.getAlertsSnoozedUntil().isBefore(now))
                .filter(u -> isInactive(u, now))
                .toList();

            if (inactiveUsers.isEmpty()) {
                log.debug("No inactive users found.");
                return;
            }

            log.info("Found {} inactive user(s).", inactiveUsers.size());
            for (User user : inactiveUsers) {
                long effectiveInactiveMs = effectiveInactiveMs(user);
                Instant effectiveCutoff = now.minusMillis(effectiveInactiveMs);
                Instant lastActivity = latestActivity(user);
                if (lastActivity != null && !lastActivity.isBefore(effectiveCutoff)) {
                    continue;
                }
                long actualInactiveMs = lastActivity != null ? now.toEpochMilli() - lastActivity.toEpochMilli() : effectiveInactiveMs;

                int contactCount = emergencyContactRepository.findByUserIdOrderByContactIndexAsc(user.getId()).size();
                if (contactCount < 1) continue;

                int escalationLevel = computeEscalationLevel(user, now);
                int contactsToNotify = Math.min(escalationLevel, contactCount);
                int currentCount = user.getContactsAlertedCount() != null ? user.getContactsAlertedCount().intValue() : 0;

                if (contactsToNotify <= currentCount) continue;

                user.setFirstAlertSentAt(user.getFirstAlertSentAt() != null ? user.getFirstAlertSentAt() : now);
                user.setContactsAlertedCount((short) contactsToNotify);
                userRepository.save(user);

                log.info("Inactive user: ID={}, Email={}, LastLogin={}, escalation={}/{}",
                        user.getId(), user.getEmail(), user.getLastLoginDate(), contactsToNotify, contactCount);

                boolean sendSms = shouldSendSms(user);
                boolean sendEmail = shouldSendEmail(user);
                if (sendSms) {
                    String smsMessage = String.format(
                            "Alert: User %s (%s) has been inactive. Last activity: %s",
                            user.getEmail(), user.getId(), lastActivity);
                    emergencyContactService.sendSmsToContactsUpTo(user.getId(), smsMessage, contactsToNotify);
                }
                if (sendEmail) {
                    emergencyContactService.sendInactiveUserAlertToContactsUpTo(user, actualInactiveMs, contactsToNotify);
                }
            }
        } catch (Exception e) {
            metrics.recordSchedulerFailure();
            log.error("Scheduler failed while checking inactive users", e);
            throw new RuntimeException(e);
        }
        });
    }

    private boolean isInactive(User user, Instant now) {
        Instant cutoff = now.minusMillis(effectiveInactiveMs(user));
        Instant last = latestActivity(user);
        return last == null || last.isBefore(cutoff);
    }

    private long effectiveInactiveMs(User user) {
        if (user.getInactivityThresholdDays() != null && user.getInactivityThresholdDays() >= 1) {
            return user.getInactivityThresholdDays() * 86_400_000L;
        }
        return defaultInactiveMs;
    }

    private Instant latestActivity(User user) {
        Instant lastLogin = user.getLastLoginDate();
        Instant lastCheckIn = user.getLastManualCheckInAt();
        if (lastLogin == null && lastCheckIn == null) return null;
        if (lastLogin == null) return lastCheckIn;
        if (lastCheckIn == null) return lastLogin;
        return lastLogin.isAfter(lastCheckIn) ? lastLogin : lastCheckIn;
    }

    private int computeEscalationLevel(User user, Instant now) {
        if (user.getFirstAlertSentAt() == null) {
            return 1;
        }
        long elapsedMs = now.toEpochMilli() - user.getFirstAlertSentAt().toEpochMilli();
        int steps = (int) (elapsedMs / escalationIntervalMs);
        return 1 + Math.max(0, steps);
    }

    private boolean shouldSendSms(User user) {
        if (!smsEnabled) return false;
        AlertChannelPreference pref = user.getAlertChannelPreference();
        return pref == null || pref == AlertChannelPreference.BOTH || pref == AlertChannelPreference.SMS;
    }

    private boolean shouldSendEmail(User user) {
        if (!emailEnabled) return false;
        AlertChannelPreference pref = user.getAlertChannelPreference();
        return pref == null || pref == AlertChannelPreference.BOTH || pref == AlertChannelPreference.EMAIL;
    }
}
