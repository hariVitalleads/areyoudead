package com.checkin.audit;

public enum AuditAction {
	LOGIN,
	PASSWORD_RESET_REQUESTED,
	PASSWORD_RESET_COMPLETED,
	/** User requested another registration verification email (unverified account). */
	RESEND_VERIFICATION_EMAIL,
	UPDATE_DETAILS,
	CHECK_IN,
	/** Super user viewed a user's audit detail. */
	ADMIN_VIEWED_USER
}

