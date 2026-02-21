package com.checkin.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Application metrics for observability.
 * Exposed via /actuator/metrics when metrics endpoint is enabled.
 */
@Component
public class AppMetrics {

	public static final String LOGINS_TOTAL = "checkin.logins.total";
	public static final String REGISTRATIONS_TOTAL = "checkin.registrations.total";
	public static final String REFRESH_TOKENS_TOTAL = "checkin.refresh_tokens.total";
	public static final String ALERTS_EMAIL_SENT = "checkin.alerts.email.sent";
	public static final String ALERTS_EMAIL_FAILED = "checkin.alerts.email.failed";
	public static final String ALERTS_SMS_SENT = "checkin.alerts.sms.sent";
	public static final String ALERTS_SMS_FAILED = "checkin.alerts.sms.failed";
	public static final String SCHEDULER_RUNS = "checkin.scheduler.runs";
	public static final String SCHEDULER_FAILURES = "checkin.scheduler.failures";
	public static final String CHECK_INS_TOTAL = "checkin.check_ins.total";

	private final Counter loginsTotal;
	private final Counter registrationsTotal;
	private final Counter refreshTokensTotal;
	private final Counter alertsEmailSent;
	private final Counter alertsEmailFailed;
	private final Counter alertsSmsSent;
	private final Counter alertsSmsFailed;
	private final Counter schedulerRuns;
	private final Counter schedulerFailures;
	private final Counter checkInsTotal;
	private final Timer schedulerDuration;

	public AppMetrics(MeterRegistry registry) {
		this.loginsTotal = registry.counter(LOGINS_TOTAL);
		this.registrationsTotal = registry.counter(REGISTRATIONS_TOTAL);
		this.refreshTokensTotal = registry.counter(REFRESH_TOKENS_TOTAL);
		this.alertsEmailSent = registry.counter(ALERTS_EMAIL_SENT);
		this.alertsEmailFailed = registry.counter(ALERTS_EMAIL_FAILED);
		this.alertsSmsSent = registry.counter(ALERTS_SMS_SENT);
		this.alertsSmsFailed = registry.counter(ALERTS_SMS_FAILED);
		this.schedulerRuns = registry.counter(SCHEDULER_RUNS);
		this.schedulerFailures = registry.counter(SCHEDULER_FAILURES);
		this.checkInsTotal = registry.counter(CHECK_INS_TOTAL);
		this.schedulerDuration = registry.timer("checkin.scheduler.duration");
	}

	public void recordLogin() {
		loginsTotal.increment();
	}

	public void recordRegistration() {
		registrationsTotal.increment();
	}

	public void recordRefreshToken() {
		refreshTokensTotal.increment();
	}

	public void recordAlertEmailSent() {
		alertsEmailSent.increment();
	}

	public void recordAlertEmailFailed() {
		alertsEmailFailed.increment();
	}

	public void recordAlertSmsSent() {
		alertsSmsSent.increment();
	}

	public void recordAlertSmsFailed() {
		alertsSmsFailed.increment();
	}

	public void recordSchedulerRun() {
		schedulerRuns.increment();
	}

	public void recordSchedulerFailure() {
		schedulerFailures.increment();
	}

	public void recordCheckIn() {
		checkInsTotal.increment();
	}

	/** Returns the scheduler duration timer for manual recording. */
	public Timer getSchedulerDurationTimer() {
		return schedulerDuration;
	}
}
