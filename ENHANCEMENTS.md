Lets now work on 3, 4, 6, 9, 10, 11 and 12 

# Checkin Application — Enhancement Suggestions

Recommendations for improving the Checkin app, grouped by area and priority.

---

## Completed (2025-01)

- **#3 Security**: Refresh tokens, rate limiting (Bucket4j) on auth endpoints
- **#5 User Check-In Flow**: Custom inactivity thresholds per user; escalation (contact 1 → 2 → 3); contact labels (Primary, Secondary)
- **#4 Security Config**: Fixed permitAll to match actual endpoints; added /api/auth/refresh
- **#6 Emergency Contacts**: Opt-out link in emails; manual check-in endpoint; filter opted-out contacts
- **#9 Health**: DB + mail health; actuator probes
- **#10 Structured Logging**: Request ID (X-Request-ID), userId in MDC, logback pattern
- **#11 Metrics**: Logins, registrations, refresh, alerts (email/SMS sent/failed), scheduler runs/failures, check-ins
- **#12 Alerting**: Scheduler failure counter; email/SMS failure counters (for Prometheus/Alertmanager)

---

## Critical / High Priority

### 1. Fix Scheduler Logic
The scheduler currently has debug code that only targets one hardcoded user (`user_tmuara@example.com`) instead of processing all inactive users. The real `inactiveUsers` query result is ignored. Restore the loop so all inactive users get emergency contact alerts.

### 2. Consolidate or Document API Duplication
Three parallel APIs exist: `/api/auth`, `/api/login`, and `/api/user` with overlapping endpoints (register, login, forgot-password, etc.). Either:
- Consolidate to a single canonical API, or
- Clearly document which is canonical vs. legacy/alias.

### 3. Security Enhancements
- **Refresh tokens** — Add refresh token flow for long-lived sessions without storing passwords.
- **Rate limiting** — Throttle login, forgot-password, and register endpoints.
- **CSRF** — If using cookie-based auth, enable CSRF protection; for JWT-only, keep disabled.

### 4. Security Config Cleanup
Remove or fix references to non-existent endpoints in `SecurityConfig`:
- `/api/login/signup` and `/api/user/signup` are permitted but don’t exist (only `/register` does).

---

## Features

### 5. User "Check-In" Flow
- Add a manual **"I'm okay"** check-in button that updates `last_login_date`.
- Let users configure **custom inactivity thresholds** (e.g. 3 days vs 14 days).
- **Escalation flow** — If contact 1 doesn’t respond, notify contact 2, etc.
- **Contact priority/order** — Primary, secondary, etc.

### 6. Emergency Contact Improvements
- **Verify contact email** before adding (confirmation email).
- **Opt-out for contacts** — Allow emergency contacts to opt out of future alerts.
- **"All clear"** — User can dismiss or cancel an impending alert.

### 7. SMS Integration
- Integrate Twilio, AWS SNS, or similar for real SMS delivery.
- Make **SMS vs email** configurable per contact.

### 8. Push Notifications
- Mobile push to remind users to check in before alerting contacts.

---

## Observability & Operations

### 9. Health Checks
- Database connectivity.
- External services (email/SMS) in health endpoint.
- Separate readiness vs. liveness (e.g. actuator health groups).

### 10. Structured Logging
- Request IDs (MDC).
- User ID in log context for audit trail.
- Log levels configurable per environment.

### 11. Metrics
- Counters for logins, registrations, alerts sent.
- Scheduler run metrics (duration, users processed).
- Failed auth attempt metrics.

### 12. Alerting
- Monitor scheduler failures.
- Alert on email/SMS delivery failures.

---

## Code Quality

### 13. OpenAPI / Swagger
- Serve Swagger UI at `/swagger-ui.html`.
- Keep `openapi.yaml` in sync with controllers.
- Optionally generate client SDKs from OpenAPI.

### 14. Test Coverage
- Integration tests with `@SpringBootTest`.
- Security integration tests.
- More edge cases in `EmergencyContactServiceTest` and scheduler tests.

### 15. Configuration Management
- Environment-specific profiles (dev/stage/prod).
- External config (Vault, AWS Secrets Manager) for JWT secret, DB credentials.
- Validate required config on startup (e.g. `@ConfigurationProperties` + `@Validated`).

---

## Frontend

### 16. Missing Pages
- Implement `/account` and `/emergency-contacts` (linked in Dashboard but may not exist or be incomplete).
- Account details and emergency contact management UIs.

### 17. UX Improvements
- Loading and error states.
- Form validation with clear error messages.
- Responsive layout.
- Dark mode.

### 18. Auth Flow
- Token refresh handling.
- Redirect to login on 401.
- Clear logout behavior.

---

## DevOps & Deployment

### 19. Docker
- Dockerfile for the Spring Boot app.
- Full `docker-compose` stack (app, DB, MailHog) for local and CI.

### 20. CI/CD
- Run tests on every push.
- Build and deploy to staging/production.
- Jenkins for tests (if free) — per TODO.

### 21. Database
- Tune connection pool (HikariCP).
- Index `last_login_date` and other query-heavy columns.
- Read replicas for scaling if needed.

---

## Miscellaneous

### 22. API Versioning
- Introduce versioning (e.g. `/api/v1/`) for future changes.

### 23. Dependency Updates
- Upgrade to Gradle 8+.
- Fix JwtService deprecation warnings.
- Use Dependabot or Renovate for automated updates.

### 24. Documentation
- Update README (`/api/me` example, correct auth paths).
- Document all env vars.
- Runbook for common ops tasks.

---

## Suggested Order

1. Fix scheduler so it processes all inactive users.
2. Consolidate or document auth APIs and fix security config.
3. Add refresh tokens and rate limiting.
4. Complete frontend pages and auth flow.
5. Improve observability (health, metrics, alerting).
