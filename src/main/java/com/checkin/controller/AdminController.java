package com.checkin.controller;

import com.checkin.audit.AuditAction;
import com.checkin.dto.AdminUserAuditResponse;
import com.checkin.dto.AdminUserSummaryResponse;
import com.checkin.security.CurrentUser;
import com.checkin.security.UserPrincipal;
import com.checkin.service.AdminService;
import com.checkin.service.AuditService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin audit API. Requires ROLE_SUPER_USER. Super users log in via POST /api/user/login
 * (same as regular users); the flag super_user=true on their account grants access.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
	private final AdminService adminService;
	private final AuditService auditService;

	public AdminController(AdminService adminService, AuditService auditService) {
		this.adminService = adminService;
		this.auditService = auditService;
	}

	/**
	 * List all users for audit purposes (newest first). Excludes sensitive data.
	 */
	@GetMapping("/users")
	public List<AdminUserSummaryResponse> listUsers() {
		return adminService.listUsers();
	}

	/**
	 * Full audit view for a user: details, audit events, emergency contacts.
	 * Records ADMIN_VIEWED_USER for audit trail.
	 */
	@GetMapping("/users/{userId}")
	public AdminUserAuditResponse getUserAuditDetail(@CurrentUser UserPrincipal principal, @PathVariable UUID userId) {
		AdminUserAuditResponse response = adminService.getUserAuditDetail(userId);
		auditService.record(principal.getUserId(), AuditAction.ADMIN_VIEWED_USER, "viewed user " + userId);
		return response;
	}
}
