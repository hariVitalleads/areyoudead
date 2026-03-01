package com.checkin.dto;

import java.time.Instant;

/**
 * Summary of a user's check-in activity from audit_events.
 */
public class CheckInSummaryResponse {
	private final long totalCheckIns;
	private final Instant lastCheckInAt;
	private final long checkInsLast7Days;
	private final long checkInsLast30Days;

	public CheckInSummaryResponse(long totalCheckIns, Instant lastCheckInAt, long checkInsLast7Days,
			long checkInsLast30Days) {
		this.totalCheckIns = totalCheckIns;
		this.lastCheckInAt = lastCheckInAt;
		this.checkInsLast7Days = checkInsLast7Days;
		this.checkInsLast30Days = checkInsLast30Days;
	}

	public long getTotalCheckIns() {
		return totalCheckIns;
	}

	public Instant getLastCheckInAt() {
		return lastCheckInAt;
	}

	public long getCheckInsLast7Days() {
		return checkInsLast7Days;
	}

	public long getCheckInsLast30Days() {
		return checkInsLast30Days;
	}
}
