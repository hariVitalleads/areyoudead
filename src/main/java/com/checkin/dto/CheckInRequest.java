package com.checkin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Optional body for check-in. When snoozeDays is provided, alerts are snoozed until that many days from now.
 */
public class CheckInRequest {

	/** Optional: snooze inactivity alerts for this many days (e.g. 7 = don't alert for 7 days). */
	@Min(1)
	@Max(90)
	private Integer snoozeDays;

	public Integer getSnoozeDays() {
		return snoozeDays;
	}

	public void setSnoozeDays(Integer snoozeDays) {
		this.snoozeDays = snoozeDays;
	}
}
