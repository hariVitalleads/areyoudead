package com.checkin.service;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.loader.ClasspathLoader;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Renders the emergency contact email template for inactive user alerts using Pebble.
 */
@Component
public class InactiveUserEmailTemplate {
    private static final Logger log = LoggerFactory.getLogger(InactiveUserEmailTemplate.class);
    private static final String TEMPLATE_NAME = "emergency-contact-inactive-alert.peb";

    private final PebbleEngine pebbleEngine;

    public InactiveUserEmailTemplate() {
        ClasspathLoader loader = new ClasspathLoader(getClass().getClassLoader());
        loader.setPrefix("templates");
        loader.setSuffix(".peb");
        this.pebbleEngine = new PebbleEngine.Builder()
                .loader(loader)
                .autoEscaping(true)
                .build();
    }

    /**
     * Renders the template with the given variables.
     *
     * @param userEmail      the inactive user's email
     * @param userId         the inactive user's ID
     * @param lastLoginDate  when the user last logged in (may be null)
     * @param inactiveMs     how long the user has been inactive (ms)
     * @return HTML email body
     */
    public String render(String userEmail, String userId, Instant lastLoginDate, long inactiveMs) {
        String lastLoginStr = lastLoginDate != null
                ? DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
                        .withZone(ZoneOffset.UTC)
                        .format(lastLoginDate)
                : "never";
        String durationStr = formatDuration(inactiveMs);
        long inactiveDays = inactiveMs / 86_400_000;

        Map<String, Object> context = new HashMap<>();
        context.put("userEmail", userEmail);
        context.put("userId", userId);
        context.put("lastLoginDate", lastLoginStr);
        context.put("inactiveDuration", durationStr);
        context.put("inactiveDays", inactiveDays);

        try (StringWriter writer = new StringWriter()) {
            pebbleEngine.getTemplate(TEMPLATE_NAME.replace(".peb", ""))
                    .evaluate(writer, context);
            String html = writer.toString();
            if (log.isDebugEnabled()) {
                log.debug("Rendered inactive user email template:\n{}", html);
            }
            return html;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render email template", e);
        }
    }

    private static String formatDuration(long ms) {
        if (ms < 60_000) {
            return (ms / 1_000) + " seconds";
        }
        if (ms < 3_600_000) {
            return (ms / 60_000) + " minutes";
        }
        if (ms < 86_400_000) {
            return (ms / 3_600_000) + " hours";
        }
        return (ms / 86_400_000) + " days";
    }
}
