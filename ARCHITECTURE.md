# Checkin — Architecture Diagram

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.4 (Java 17) |
| Security | Spring Security + JWT (jjwt 0.12) + refresh tokens |
| Persistence | Spring Data JPA + Flyway + PostgreSQL |
| Scheduling | Spring Scheduler |
| Email | Spring Mail + MailHog (dev) / SMTP (prod) |
| Rate limiting | Bucket4j |
| Build | Gradle |
| Infra | Docker Compose |

**Context path:** `/checkin` — all APIs are under `http://host:port/checkin/api/...`

---

## Component Diagram

```mermaid
flowchart TD
    Client["🌐 HTTP Client"]

    subgraph Security["Security Layer"]
        RateLimit["RateLimitFilter\n(Bucket4j per IP)"]
        JwtFilter["JwtAuthenticationFilter\n(OncePerRequestFilter)"]
        JwtSvc["JwtService\n(create/parse JWT, refresh)"]
        UP["UserPrincipal"]
        SC["SecurityConfig\n(route rules, CORS, BCrypt)"]
    end

    subgraph Controllers["REST Controllers"]
        UC["AppUserController\n/api/user · canonical\n POST /register, /login, /refresh\n GET /verify-email/{token}\n POST /forgot-password\n POST /reset-password\n GET /me · PUT /details\n POST /check-in"]
        LC["LoginController\n/api/login · alias\n POST /login"]
        EC["EmergencyContactController\n/api/emergency-contacts\n GET / POST / PUT/{id} / DELETE/{id}\n GET /verify/{token}\n GET /opt-out/{token}"]
    end

    subgraph Services["Service Layer"]
        AuthSvc["AuthService\n(register, login, refresh,\nverifyUserByToken,\nsend verification email)"]
        LoginSvc["LoginService\n(forgotPassword, resetPassword)"]
        AppUserSvc["AppUserService\n(me, update, checkIn)"]
        EmgSvc["EmergencyContactService\n(CRUD, verifyByToken, optOutByToken,\nsendSmsToContactsUpTo,\nsendInactiveUserAlertToContactsUpTo)"]
        AuditSvc["AuditService"]
    end

    subgraph Scheduler["Scheduler"]
        Sched["InactiveUserScheduler\n(@Scheduled every 10s)\nper-user inactivity threshold\nescalation: contact 1 → 2 → 3\nSMS + email to emergency contacts"]
    end

    subgraph Repositories["Repository Layer"]
        UR["UserRepository"]
        RegR["RegistrationRepository"]
        ECR["EmergencyContactRepository"]
        RefreshR["RefreshTokenRepository"]
        AuditR["AuditEventRepository"]
    end

    subgraph Models["JPA Entities"]
        UserM["User\nid, email, passwordHash\nlastLoginDate, lastManualCheckInAt\nalertsSnoozedUntil\nemail verification fields\ninactivityThresholdDays\nfirstAlertSentAt, contactsAlertedCount"]
        RegM["Registration\npersonal details, registrationType"]
        ECM["EmergencyContact\nuserId, contactIndex, mobileNumber\nemail, label, verifiedAt\noptOutToken, optedOutAt"]
        RefreshM["RefreshToken"]
        AuditM["AuditEvent"]
    end

    DB[("🐘 PostgreSQL")]

    Client --> RateLimit
    RateLimit --> JwtFilter
    JwtFilter --> JwtSvc
    JwtSvc --> UP
    JwtFilter --> SC
    SC --> Controllers

    UC --> AuthSvc
    UC --> LoginSvc
    UC --> AppUserSvc
    LC --> AuthSvc
    EC --> EmgSvc

    AuthSvc --> AuditSvc
    AppUserSvc --> AuditSvc

    AuthSvc --> UR
    AuthSvc --> RegR
    AuthSvc --> RefreshR
    AppUserSvc --> UR
    LoginSvc --> UR
    LoginSvc --> RegR
    EmgSvc --> ECR
    AuditSvc --> AuditR

    Sched --> UR
    Sched --> ECR
    Sched --> EmgSvc

    UR --> DB
    RegR --> DB
    ECR --> DB
    RefreshR --> DB
    AuditR --> DB

    DB -.-> UserM
    DB -.-> RegM
    DB -.-> ECM
    DB -.-> RefreshM
    DB -.-> AuditM
```

---

## Package Structure

```
com.checkin
├── CheckinApplication
├── audit/
│   └── AuditAction                 ← LOGIN, CHECK_IN, UPDATE_DETAILS, …
├── config/
│   ├── AppMetrics                  ← Micrometer counters/timers
│   ├── EmailProperties             ← app.emergency-contacts.email
│   ├── EmergencyContactLimitProperties
│   ├── EmergencyContactProperties  ← SMS/email enabled
│   ├── JwtProperties
│   ├── MdcFilter                   ← Request ID, userId in MDC
│   ├── RateLimitFilter             ← Bucket4j on auth paths
│   ├── RateLimitProperties
│   ├── SecurityConfig
│   ├── UserVerificationProperties  ← app.user (email verification)
│   └── WebMvcConfig                ← UserPrincipalArgumentResolver
├── controller/
│   ├── AppUserController           ← /api/user/* (canonical)
│   ├── EmergencyContactController  ← /api/emergency-contacts/*
│   └── LoginController             ← /api/login (alias for login only)
├── dto/                            ← Request/Response POJOs
├── exception/
│   └── GlobalExceptionHandler
├── model/
│   ├── AuditEvent
│   ├── EmergencyContact
│   ├── Registration
│   ├── RefreshToken
│   └── User
├── repository/                     ← UserRepository, RegistrationRepository,
│                                    EmergencyContactRepository, RefreshTokenRepository,
│                                    AuditEventRepository
├── scheduler/
│   └── InactiveUserScheduler       ← Every 10s, per-user threshold, escalation
├── security/
│   ├── CurrentUser
│   ├── JwtAuthenticationFilter
│   ├── JwtService
│   ├── UserPrincipal
│   └── UserPrincipalArgumentResolver
└── service/
    ├── AppUserService
    ├── AuditService
    ├── AuthService
    ├── EmergencyContactService
    ├── EmergencyContactVerificationTemplate
    ├── InactiveUserEmailTemplate
    ├── LoginService
    ├── UserVerificationTemplate
    └── (templates in resources/templates/*.peb)
```

---

## API Overview (canonical: `/api/user`)

| Endpoint | Auth | Description |
|----------|------|-------------|
| `POST /api/user/register` | No | Register; sends verification email if enabled |
| `POST /api/user/login` | No | Login; requires verified email when enabled |
| `POST /api/user/refresh` | No | Refresh access token |
| `GET /api/user/verify-email/{token}` | No | Verify user email (from registration email) |
| `POST /api/user/forgot-password` | No | Request password reset |
| `POST /api/user/reset-password` | No | Reset password with token |
| `GET /api/user/me` | Yes | Current user details + inactivityThresholdDays |
| `PUT /api/user/details` | Yes | Update email, inactivityThresholdDays |
| `POST /api/user/check-in` | Yes | Manual check-in; optional snoozeDays (1–90) |
| `POST /api/login/login` | No | Alias for /api/user/login |

---

## Key Flows

### 1. Registration
`POST /api/user/register` → `AuthService.register` → saves `User` + `Registration` → sends verification email (if `APP_BASE_URL` set) → returns `UserResponse`

### 2. Email Verification
User clicks link in email → `GET /api/user/verify-email/{token}` → `AuthService.verifyUserByToken` → sets `email_verified_at`, clears token

### 3. Login
`POST /api/user/login` → `AuthService.login` → validates credentials → rejects if email not verified (when required) → records `AuditEvent(LOGIN)` → returns `AuthResponse` (accessToken + refreshToken)

### 4. Refresh
`POST /api/user/refresh` → `AuthService.refresh` → validates refresh token → issues new access + refresh tokens

### 5. Check-in
`POST /api/user/check-in` → `AppUserService.checkIn` → updates `lastManualCheckInAt`; optional `snoozeDays` sets `alertsSnoozedUntil`; clears `firstAlertSentAt`, `contactsAlertedCount`

### 6. Inactive-User Alerting (Scheduler)
Every 10s → `InactiveUserScheduler` → finds users inactive (per-user threshold or global default) → **escalation**: first run notifies contact 1; after `escalation-interval-hours` adds contact 2; then 3 → `sendSmsToContactsUpTo` + `sendInactiveUserAlertToContactsUpTo`

### 7. Emergency Contact Verification
On add → sends verification email → `GET /api/emergency-contacts/verify/{token}` → sets `verified_at`. Only verified contacts receive alerts when `require-verification` is true.

### 8. Emergency Contact Opt-out
`GET /api/emergency-contacts/opt-out/{token}` (from link in alert email) → sets `opted_out_at`; opted-out contacts excluded from future alerts
