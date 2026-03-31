package com.checkin.service;

import com.checkin.config.FcmProperties;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Base64;

@Service
public class FcmService {
	private static final Logger log = LoggerFactory.getLogger(FcmService.class);

	private final FirebaseMessaging firebaseMessaging;
	private final boolean enabled;

	public FcmService(FcmProperties props) {
		this.enabled = props.isEnabled();
		FirebaseMessaging fm = null;
		if (enabled) {
			try {
				GoogleCredentials credentials = loadCredentials(props);
				if (credentials != null) {
					FirebaseOptions options = FirebaseOptions.builder()
							.setCredentials(credentials)
							.build();
					FirebaseApp.initializeApp(options);
					fm = FirebaseMessaging.getInstance();
				}
			} catch (Exception e) {
				log.error("Failed to initialize Firebase", e);
			}
		}
		this.firebaseMessaging = fm;
	}

	private static GoogleCredentials loadCredentials(FcmProperties props) throws IOException {
		InputStream is = null;
		if (props.getCredentialsJsonBase64() != null && !props.getCredentialsJsonBase64().isBlank()) {
			byte[] decoded = Base64.getDecoder().decode(props.getCredentialsJsonBase64().trim());
			is = new ByteArrayInputStream(decoded);
		} else if (props.getCredentialsPath() != null && !props.getCredentialsPath().isBlank()) {
			is = new FileInputStream(props.getCredentialsPath().trim());
		}
		if (is == null) {
			return null;
		}
		return GoogleCredentials.fromStream(is);
	}

	public void sendCheckinReminder(String fcmToken, String title, String body) {
		if (!enabled || firebaseMessaging == null || fcmToken == null || fcmToken.isBlank()) {
			return;
		}
		try {
			Message message = Message.builder()
					.setToken(fcmToken)
					.setNotification(Notification.builder()
							.setTitle(title)
							.setBody(body)
							.build())
					.putData("type", "checkin_reminder")
					.putData("scheduledAt", Instant.now().toString())
					.setAndroidConfig(AndroidConfig.builder()
							.setPriority(AndroidConfig.Priority.HIGH)
							.setNotification(AndroidNotification.builder()
									.setChannelId("checkin_reminders")
									.setSound("alarm")
									.build())
							.build())
					.build();
			firebaseMessaging.send(message);
		} catch (FirebaseMessagingException e) {
			log.warn("FCM send failed for token (length={}): {}", fcmToken.length(), e.getMessage());
			// Optionally clear invalid token in DB - caller could handle retry/clear
		}
	}
}
