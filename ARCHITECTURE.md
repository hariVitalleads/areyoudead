# Checkin — Architecture

## Tech Stack

| Layer | Technology |
|-------|-------------|
| **Backend** | Spring Boot 3.4 (Java 17) |
| **Frontend** | Vite + React 19 + TypeScript |
| **Security** | Spring Security + JWT (jjwt 0.12) + refresh tokens |
| **Persistence** | Spring Data JPA + Flyway + PostgreSQL |
| **Scheduling** | Spring Scheduler |
| **Email** | Spring Mail + MailHog (dev) / SES SMTP (prod) |
| **Rate limiting** | Bucket4j |
| **Build** | Gradle (backend) · npm (frontend) |
| **Infra** | Docker Compose · AWS (ECS, ECR, RDS, S3, CloudFront) |

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
        AC["AdminController\n/api/admin · super_user only\n GET /users · GET /users/{id}"]
    end

    subgraph Services["Service Layer"]
        AuthSvc["AuthService\n(register, login, refresh,\nverifyUserByToken,\nsend verification email)"]
        LoginSvc["LoginService\n(forgotPassword, resetPassword)"]
        AppUserSvc["AppUserService\n(me, update, checkIn)"]
        AdminSvc["AdminService\n(listUsers, getUserAuditDetail)"]
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
    AC --> AdminSvc
    AC --> AuditSvc
    EC --> EmgSvc

    AuthSvc --> AuditSvc
    AppUserSvc --> AuditSvc

    AuthSvc --> UR
    AuthSvc --> RegR
    AuthSvc --> RefreshR
    AppUserSvc --> UR
    AdminSvc --> UR
    AdminSvc --> AuditR
    AdminSvc --> ECR
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

## Deployment Architecture (AWS)

```mermaid
flowchart LR
    subgraph Client["Client"]
        Browser["Browser"]
    end

    subgraph Frontend["Frontend (SPA)"]
        direction TB
        CF["CloudFront\n(optional)"]
        S3["S3\nstatic dist/"]
        ECS_F["ECS + nginx\n(optional)"]
        CF --> S3
    end

    subgraph API["API"]
        ALB["ALB"]
        ECS_A["ECS / EC2\nSpring Boot :8080"]
        ALB --> ECS_A
    end

    subgraph Data["Data"]
        RDS[("RDS\nPostgreSQL")]
    end

    subgraph Email["Email"]
        SES["Amazon SES"]
    end

    Browser --> CF
    Browser --> ECS_F
    Browser --> ALB
    ECS_A --> RDS
    ECS_A --> SES
```

**Frontend deployment options:**
- **S3 + CloudFront** — Build `dist/`, sync to S3, serve via CloudFront (recommended for static SPAs)
- **ECS** — Docker image (nginx + built `dist/`), run on Fargate

**API:** ECS Fargate or EC2, behind ALB, connects to RDS and SES.

See [DEPLOYMENT-AWS.md](DEPLOYMENT-AWS.md) for detailed steps.

---

## Frontend

| Item | Description |
|------|-------------|
| **Stack** | Vite 7, React 19, React Router |
| **Location** | `frontend/` |
| **Build** | `npm run build` → `dist/` |
| **API base** | `VITE_API_URL` (baked at build) — e.g. `http://localhost:8080/checkin` |
| **Dev** | `npm run dev` — Vite proxy `/api` → `http://localhost:8080/checkin/api` |
| **Prod serve** | nginx or `npx serve -s dist` (SPA fallback) |

---

## Docker Compose (Local / Prod)

| Service | Port | Description |
|---------|------|--------------|
| `app` | 8080 | Spring Boot API |
| `frontend` | 3000 | nginx serving built SPA |
| `postgres` | 5432 | PostgreSQL 16 |
| `mail` (dev) | 1025, 8025 | MailHog SMTP + Web UI |

```bash
docker compose -f docker-compose.prod.yml up -d
# API: http://localhost:8080/checkin  |  Frontend: http://localhost:3000
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
│   ├── AdminController              ← /api/admin/* (super_user only)
│   ├── AppUserController            ← /api/user/* (canonical)
│   ├── EmergencyContactController   ← /api/emergency-contacts/*
│   └── LoginController              ← /api/login (alias for login only)
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
    ├── AdminService
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

## API Overview

### User API (canonical: `/api/user`)

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

### Emergency Contacts (`/api/emergency-contacts`)

| Endpoint | Auth | Description |
|----------|------|-------------|
| `GET /api/emergency-contacts` | Yes | List user's contacts |
| `POST /api/emergency-contacts` | Yes | Add contact; sends verification email |
| `PUT /api/emergency-contacts/{id}` | Yes | Update contact |
| `DELETE /api/emergency-contacts/{id}` | Yes | Delete contact |
| `GET /api/emergency-contacts/verify/{token}` | No | Verify contact email (from link) |
| `GET /api/emergency-contacts/opt-out/{token}` | No | Opt out of alerts (from link) |

### Admin (`/api/admin` — requires `ROLE_SUPER_USER`)

| Endpoint | Auth | Description |
|----------|------|-------------|
| `GET /api/admin/users` | Super user | List all users (summary) |
| `GET /api/admin/users/{userId}` | Super user | Full audit view (user, events, contacts) |

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
