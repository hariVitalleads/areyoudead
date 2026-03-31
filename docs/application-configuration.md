# Application configuration (`application.yml`)

This document describes the flags and properties in `src/main/resources/application.yml`. Spring uses **`${ENV_VAR:default}`**: if the environment variable is set, it wins; otherwise the value after `:` is used.

**Note:** With `SPRING_PROFILES_ACTIVE=prod`, `application-prod.yml` layers on top of this file for production-specific defaults.

---

## `server`

| Property | Meaning |
|----------|--------|
| **`port: 8080`** | HTTP port for the embedded server. |
| **`servlet.context-path: /checkin`** | Every URL is under `/checkin` (e.g. API is `/checkin/api/...`, actuator is `/checkin/actuator/...`). |

---

## `app.rate-limit`

Used by the rate-limit filter on auth-style endpoints (login, register, forgot-password, resend verification, etc.).

| Property | Environment variable | Default | Meaning |
|----------|------------------------|---------|--------|
| **`auth-requests-per-window`** | `RATE_LIMIT_AUTH_REQUESTS` | `10` | Max requests per IP per window for those paths. |
| **`window-seconds`** | `RATE_LIMIT_WINDOW_SECONDS` | `60` | Length of that window in seconds. |

Exceeding the limit returns **429 Too Many Requests**.

---

## `app.scheduler`

Feeds the **inactive-user** scheduler (emergency contacts when someone has not checked in).

| Property | Environment variable | Default | Meaning |
|----------|------------------------|---------|--------|
| **`inactive-ms`** | `APP_SCHEDULER_INACTIVE_MS` | `86400000` (24h) | Baseline inactive window in milliseconds when the user has no custom threshold. |
| **`escalation-interval-hours`** | `APP_SCHEDULER_ESCALATION_INTERVAL_HOURS` | `24` | Hours between escalation steps (e.g. notify the next contact). |
| **`emergency-contacts.sms.enabled`** | `APP_SCHEDULER_EMERGENCY_CONTACTS_SMS_ENABLED` | `true` | Whether the scheduler may send **SMS** to emergency contacts for inactivity. |
| **`emergency-contacts.email.enabled`** | `APP_SCHEDULER_EMERGENCY_CONTACTS_EMAIL_ENABLED` | `true` | Whether it may send **email** alerts for inactivity. |

The user’s `EMAIL` / `SMS` / `BOTH` preference still applies on top of these flags.

---

## `app.user`

Registration and login verification.

| Property | Environment variable | Default | Meaning |
|----------|------------------------|---------|--------|
| **`require-email-verification`** | `USER_REQUIRE_EMAIL_VERIFICATION` | `true` | If `true`, new users must verify email before login; if `false`, they are marked verified at signup. |
| **`verification-token-ttl-seconds`** | `USER_VERIFICATION_TTL` | `86400` (24h) | How long the **registration** verification link token is valid. |

---

## `app.fcm`

Firebase Cloud Messaging (check-in reminder pushes).

| Property | Environment variable | Default | Meaning |
|----------|------------------------|---------|--------|
| **`enabled`** | `FCM_ENABLED` | `false` | Turn FCM sending on; requires credentials. |
| **`credentials-path`** | `FCM_CREDENTIALS_PATH` | *(empty)* | Filesystem path to Firebase service account JSON. |
| **`credentials-json-base64`** | `FCM_CREDENTIALS_JSON_BASE64` | *(empty)* | Same JSON, base64-encoded (e.g. containers or secrets managers). |

See also [FCM-BACKEND-IMPLEMENTATION.md](./FCM-BACKEND-IMPLEMENTATION.md).

---

## `app.emergency-contacts`

Emergency contact features (add, verify, inactivity alerts).

| Property | Environment variable | Default | Meaning |
|----------|------------------------|---------|--------|
| **`max-count`** | `EMERGENCY_CONTACTS_MAX_COUNT` | `3` | Maximum contacts per user. |
| **`require-verification`** | `EMERGENCY_CONTACTS_REQUIRE_VERIFICATION` | `true` | If `true`, contacts should verify email before receiving alerts (where the flow enforces it). |
| **`verification-token-ttl-seconds`** | `EMERGENCY_CONTACTS_VERIFICATION_TTL` | `86400` | TTL for **emergency contact** verification links (separate from user registration). |
| **`sms.enabled`** | `EMERGENCY_CONTACTS_SMS_ENABLED` | `true` | Allow SMS in the emergency-contact feature path. |
| **`email.enabled`** | `EMERGENCY_CONTACTS_EMAIL_ENABLED` | `true` | Allow email in that path. |
| **`email.from-address`** | `EMERGENCY_CONTACTS_EMAIL_FROM` | `noreply@checkin.com` | `From:` address for those emails. |
| **`email.subject-prefix`** | `EMERGENCY_CONTACTS_EMAIL_SUBJECT_PREFIX` | `[Checkin]` | Prefix on email subjects. |
| **`email.app-base-url`** | `APP_BASE_URL` | `http://localhost:8080/checkin` | Public base URL for links (verify contact, opt-out, etc.). In production, set to your real HTTPS API base (usually including `/checkin`). |

---

## `spring.datasource`, `spring.jpa`, `spring.flyway`

| Property | Meaning |
|----------|--------|
| **`spring.datasource.url` / `username` / `password`** | PostgreSQL connection; override with `SPRING_DATASOURCE_*`. |
| **`jpa.hibernate.ddl-auto: validate`** | Hibernate checks the schema matches entities; it does **not** auto-create tables (Flyway owns migrations). |
| **`hibernate.jdbc.time_zone: UTC`** | Store and read times in UTC. |
| **`flyway.enabled: true`** | Run Flyway migrations on startup. |

---

## `spring.mail`

Outbound SMTP (registration verification, emergency contact mail, etc.). Defaults suit **MailHog** on `localhost:1025` for local development.

| Area | Meaning |
|------|--------|
| **`host` / `port`** | SMTP server; production typically uses `MAIL_HOST`, `MAIL_PORT` (e.g. 587 for SES). |
| **`MAIL_SMTP_AUTH` / `MAIL_SMTP_STARTTLS`** | Often `false` locally; **true** for real providers (SES, Gmail, etc.). |
| **Connection timeouts** | `MAIL_CONNECTION_TIMEOUT`, `MAIL_WRITE_TIMEOUT`, `MAIL_TIMEOUT` (milliseconds). |

---

## `logging.level`

Overrides log level for specific packages (root defaults still apply).

| Logger | Environment variable | Default | Meaning |
|--------|------------------------|---------|--------|
| **`com.checkin.service.InactiveUserEmailTemplate`** | `LOG_LEVEL_EMAIL_TEMPLATE` | `DEBUG` | Verbose generated email HTML when debugging templates. |
| **`com.checkin.service.EmergencyContactService`** | `LOG_LEVEL_EMERGENCY_CONTACT` | `DEBUG` | Verbose emergency-contact flow logging. |

---

## `management` (Spring Boot Actuator)

| Property | Meaning |
|----------|--------|
| **`endpoints.web.exposure.include`** | Exposes **health**, **info**, and **metrics** over HTTP. |
| **`endpoint.health.probes.enabled`** | Enables Kubernetes-style liveness/readiness probe details on the health endpoint. |
| **`health.db` / `health.mail`** | Health aggregates include database connectivity and mail server checks. |

---

## `security.jwt`

| Property | Environment variable | Default | Meaning |
|----------|------------------------|---------|--------|
| **`secret`** | `JWT_SECRET` | *(dev placeholder in `application.yml`)* | HS256 signing key; **override in production** with a long random secret (at least 32 bytes for HS256). |
| **`issuer`** | `JWT_ISSUER` | `areyoudead` | JWT issuer claim (`iss`) / validation. |
| **`access-token-ttl-seconds`** | `JWT_TTL_SECONDS` | `300` (5 min) | Access token lifetime. |
| **`refresh-token-ttl-seconds`** | `JWT_REFRESH_TTL_SECONDS` | `86400` (24h) | Refresh token lifetime (stored server-side). |

---

## Related files

- `src/main/resources/application-prod.yml` — production overrides (Hikari, mail defaults, server bind address, etc.)
- `DEPLOYMENT-AWS.md` — deployment environment variables
