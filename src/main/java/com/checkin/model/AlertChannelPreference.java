package com.checkin.model;

/**
 * How the user wants their emergency contacts to be notified when they are inactive.
 */
public enum AlertChannelPreference {
	/** Notify contacts via email only. */
	EMAIL,

	/** Notify contacts via SMS only. */
	SMS,

	/** Notify contacts via both email and SMS. */
	BOTH
}
