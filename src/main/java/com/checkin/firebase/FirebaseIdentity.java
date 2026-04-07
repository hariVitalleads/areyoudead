package com.checkin.firebase;

/** Claims extracted from a verified Firebase ID token. */
public record FirebaseIdentity(
		String uid,
		String email,
		boolean emailVerified,
		String displayName) {}
