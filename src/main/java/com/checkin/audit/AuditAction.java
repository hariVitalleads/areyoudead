package com.checkin.audit;

public enum AuditAction {
	LOGIN,
	PASSWORD_RESET_REQUESTED,
	PASSWORD_RESET_COMPLETED,
	UPDATE_DETAILS,
	CHECK_IN,
	/** Super user viewed a user's audit detail. */
	ADMIN_VIEWED_USER
}

