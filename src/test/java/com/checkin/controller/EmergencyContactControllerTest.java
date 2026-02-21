package com.checkin.controller;

import com.checkin.dto.EmergencyContactRequest;
import com.checkin.dto.EmergencyContactResponse;
import com.checkin.security.JwtService;
import com.checkin.security.UserPrincipal;
import com.checkin.service.EmergencyContactService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmergencyContactController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class EmergencyContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmergencyContactService emergencyContactService;

    @MockitoBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID userId;
    private UUID contactId;
    private EmergencyContactResponse contactResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        contactId = UUID.randomUUID();
        contactResponse = new EmergencyContactResponse(
                contactId, "+1234567890", "contact@example.com", (short) 0);
    }

    private Authentication auth() {
        return new UsernamePasswordAuthenticationToken(
                new UserPrincipal(userId, "test@example.com"), null, null);
    }

    @Test
    void getContacts_Success() throws Exception {
        // Given
        List<EmergencyContactResponse> contacts = List.of(contactResponse);
        when(emergencyContactService.getContacts(eq(userId))).thenReturn(contacts);

        // When & Then
        mockMvc.perform(get("/api/emergency-contacts").with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(contactId.toString()))
                .andExpect(jsonPath("$[0].mobileNumber").value("+1234567890"))
                .andExpect(jsonPath("$[0].email").value("contact@example.com"));
    }

    @Test
    void getContacts_EmptyList() throws Exception {
        // Given
        when(emergencyContactService.getContacts(eq(userId))).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/emergency-contacts").with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void addContact_Success() throws Exception {
        // Given
        EmergencyContactRequest request = new EmergencyContactRequest();
        request.setMobileNumber("+1234567890");
        request.setEmail("contact@example.com");

        when(emergencyContactService.addContact(eq(userId), any(EmergencyContactRequest.class)))
                .thenReturn(contactResponse);

        // When & Then
        mockMvc.perform(post("/api/emergency-contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(auth()))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(contactId.toString()))
                .andExpect(jsonPath("$.mobileNumber").value("+1234567890"))
                .andExpect(jsonPath("$.email").value("contact@example.com"));
    }

    @Test
    void addContact_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Given
        EmergencyContactRequest request = new EmergencyContactRequest();
        // Missing required fields

        // When & Then
        mockMvc.perform(post("/api/emergency-contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(auth()))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateContact_Success() throws Exception {
        // Given
        EmergencyContactRequest request = new EmergencyContactRequest();
        request.setMobileNumber("+9876543210");
        request.setEmail("updated@example.com");

        EmergencyContactResponse updatedResponse = new EmergencyContactResponse(
                contactId, "+9876543210", "updated@example.com", (short) 0);

        when(emergencyContactService.updateContact(
                eq(userId), eq(contactId), any(EmergencyContactRequest.class)))
                .thenReturn(updatedResponse);

        // When & Then
        mockMvc.perform(put("/api/emergency-contacts/" + contactId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(auth()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mobileNumber").value("+9876543210"))
                .andExpect(jsonPath("$.email").value("updated@example.com"));
    }

    @Test
    void updateContact_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Given
        EmergencyContactRequest request = new EmergencyContactRequest();
        // Missing required fields

        // When & Then
        mockMvc.perform(put("/api/emergency-contacts/" + contactId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(auth()))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteContact_Success() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/emergency-contacts/" + contactId)
                        .with(authentication(auth()))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

}
