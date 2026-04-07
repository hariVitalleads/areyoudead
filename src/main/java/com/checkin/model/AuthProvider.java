package com.checkin.model;

/**
 * LOCAL: email/password stored in app_user. FIREBASE: identity from Firebase Authentication.
 */
public enum AuthProvider {
	LOCAL,
	FIREBASE
}
