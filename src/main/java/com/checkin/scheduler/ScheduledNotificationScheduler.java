package com.checkin.scheduler;

import com.checkin.model.User;
import com.checkin.repository.UserRepository;
import com.checkin.service.FcmService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;

@Component
public class ScheduledNotificationScheduler {
	private static final Logger log = LoggerFactory.getLogger(ScheduledNotificationScheduler.class);
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final UserRepository userRepository;
	private final FcmService fcmService;

	public ScheduledNotificationScheduler(UserRepository userRepository, FcmService fcmService) {
		this.userRepository = userRepository;
		this.fcmService = fcmService;
	}

	@Scheduled(cron = "0 * * * * *") // Every minute at second 0
	public void sendScheduledReminders() {
		Instant now = Instant.now();
		String currentTime = now.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("HH:mm"));

		List<User> users = userRepository.findAllWithFcmTokenAndSchedule();
		for (User user : users) {
			if (hasNotificationAt(user.getNotificationTimesJson(), currentTime)) {
				log.debug("Sending check-in reminder to user {}", user.getId());
				fcmService.sendCheckinReminder(
						user.getFcmToken(),
						"Check-in reminder",
						"Time to check in! Tap to open the app.");
			}
		}
	}

	private static boolean hasNotificationAt(String json, String currentTime) {
		if (json == null || json.isBlank()) return false;
		try {
			List<String> times = MAPPER.readValue(json, new TypeReference<>() {});
			return times != null && times.contains(currentTime);
		} catch (Exception e) {
			return false;
		}
	}
}
