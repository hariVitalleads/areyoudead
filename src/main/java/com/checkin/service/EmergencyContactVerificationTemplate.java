package com.checkin.service;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.loader.ClasspathLoader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class EmergencyContactVerificationTemplate {

    private static final String TEMPLATE_NAME = "emergency-contact-verify";

    private final PebbleEngine pebbleEngine;

    public EmergencyContactVerificationTemplate() {
        ClasspathLoader loader = new ClasspathLoader(getClass().getClassLoader());
        loader.setPrefix("templates");
        loader.setSuffix(".peb");
        this.pebbleEngine = new PebbleEngine.Builder()
                .loader(loader)
                .autoEscaping(true)
                .build();
    }

    public String render(String userEmail, String verifyUrl) {
        Map<String, Object> context = new HashMap<>();
        context.put("userEmail", userEmail);
        context.put("verifyUrl", verifyUrl);
        try (StringWriter writer = new StringWriter()) {
            pebbleEngine.getTemplate(TEMPLATE_NAME).evaluate(writer, context);
            return writer.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render verification email template", e);
        }
    }
}
