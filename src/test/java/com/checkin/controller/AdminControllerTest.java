package com.checkin.controller;

import com.checkin.audit.AuditAction;
import com.checkin.dto.AdminUserAuditResponse;
import com.checkin.dto.AdminUserSummaryResponse;
import com.checkin.dto.AuditEventResponse;
import com.checkin.dto.EmergencyContactResponse;
import com.checkin.model.AlertChannelPreference;
import com.checkin.security.JwtService;
import com.checkin.service.AdminService;
import com.checkin.service.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private AuditService auditService;

    /** Required by JwtAuthenticationFilter when security auto-config is loaded. */
    @MockitoBean
    private JwtService jwtService;

    @Test
    void listUsers_SuperUser_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        List<AdminUserSummaryResponse> users = List.of(
                new AdminUserSummaryResponse(userId, "user@example.com", Instant.now(), Instant.now(),
                        Instant.now(), 7, AlertChannelPreference.BOTH));
        when(adminService.listUsers()).thenReturn(users);

        mockMvc.perform(get("/api/admin/users")
                        .with(user("admin@example.com").roles("SUPER_USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("user@example.com"));
    }

    @Test
    void listUsers_NotSuperUser_Forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .with(user("user@example.com").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserAuditDetail_SuperUser_Success() throws Exception {
        UUID targetUserId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();
        AdminUserAuditResponse response = new AdminUserAuditResponse(
                targetUserId, "target@example.com", Instant.now(), Instant.now(), Instant.now(),
                null, null, 7, AlertChannelPreference.BOTH,
                List.of(new AuditEventResponse(UUID.randomUUID(), "CHECK_IN", "manual check-in", Instant.now())),
                List.of(new EmergencyContactResponse(UUID.randomUUID(), "1234567890", "contact@example.com", (short) 1, false, null)));
        when(adminService.getUserAuditDetail(targetUserId)).thenReturn(response);

        var adminPrincipal = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                new com.checkin.security.UserPrincipal(adminUserId, "admin@example.com"),
                null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SUPER_USER")));

        mockMvc.perform(get("/api/admin/users/{userId}", targetUserId)
                        .with(authentication(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetUserId.toString()))
                .andExpect(jsonPath("$.email").value("target@example.com"))
                .andExpect(jsonPath("$.auditEvents.length()").value(1))
                .andExpect(jsonPath("$.emergencyContacts.length()").value(1));

        verify(auditService).record(eq(adminUserId), eq(AuditAction.ADMIN_VIEWED_USER), eq("viewed user " + targetUserId));
    }

    @Test
    void getUserAuditDetail_UserNotFound_NotFound() throws Exception {
        UUID targetUserId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();
        when(adminService.getUserAuditDetail(targetUserId))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "user not found"));

        var adminPrincipal = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                new com.checkin.security.UserPrincipal(adminUserId, "admin@example.com"),
                null, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SUPER_USER")));

        mockMvc.perform(get("/api/admin/users/{userId}", targetUserId)
                        .with(authentication(adminPrincipal)))
                .andExpect(status().isNotFound());
    }
}
