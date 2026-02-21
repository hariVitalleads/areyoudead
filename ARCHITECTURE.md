# Checkin — Architecture Diagram

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.4 (Java 17) |
| Security | Spring Security + JWT (jjwt 0.12) |
| Persistence | Spring Data JPA + Flyway + PostgreSQL |
| Scheduling | Spring Scheduler |
| Build | Gradle |
| Infra | Docker Compose |

---

## Component Diagram

```mermaid
flowchart TD
    Client["🌐 HTTP Client"]

    subgraph Security["Security Layer"]
        JwtFilter["JwtAuthenticationFilter\n(OncePerRequestFilter)"]
        JwtSvc["JwtService\n(create / parse JWT)"]
        UP["UserPrincipal"]
        SC["SecurityConfig\n(route rules, CORS, BCrypt)"]
    end

    subgraph Controllers["REST Controllers"]
        AC["AuthController\n/api/auth\n POST /register\n POST /login"]
        LC["LoginController\n/api/login\n POST /register\n POST /login\n GET  /me\n PUT  /details\n POST /forgot-password\n POST /reset-password"]
        UC["AppUserController\n/api/user\n POST /register\n POST /forgot-password\n POST /reset-password\n GET  /me\n PUT  /details"]
        EC["EmergencyContactController\n/api/emergency-contacts\n GET / POST / PUT/{id} / DELETE/{id}"]
    end

    subgraph Services["Service Layer"]
        AuthSvc["AuthService\n(register, login)"]
        LoginSvc["LoginService\n(getAccount, updateDetails,\nforgotPassword, resetPassword)"]
        AppUserSvc["AppUserService\n(me, update)"]
        EmgSvc["EmergencyContactService\n(CRUD + sendSmsToAllContacts)"]
        AuditSvc["AuditService\n(record audit events)"]
    end

    subgraph Scheduler["Scheduler"]
        Sched["InactiveUserScheduler\n(@Scheduled daily midnight)\nfinds users inactive > N days\nalerts emergency contacts via SMS"]
    end

    subgraph Repositories["Repository Layer (Spring Data JPA)"]
        UR["UserRepository"]
        RegR["RegistrationRepository"]
        ECR["EmergencyContactRepository"]
        AuditR["AuditEventRepository"]
    end

    subgraph Models["JPA Entities (PostgreSQL)"]
        UserM["User\n(app_user table)\nid · email · passwordHash\ncreatedAt · lastLoginDate\npasswordResetTokenHash"]
        RegM["Registration\n personal details\n registrationType"]
        ECM["EmergencyContact\n userId · name · phone"]
        AuditM["AuditEvent\n userId · action · timestamp"]
    end

    DB[("🐘 PostgreSQL\n(via Docker Compose)")]

    %% Request flow
    Client -->|HTTP request| JwtFilter
    JwtFilter -->|validates Bearer token| JwtSvc
    JwtSvc -->|builds| UP
    JwtFilter -->|sets SecurityContext| SC
    SC --> Controllers

    %% Controller → Service
    AC --> AuthSvc
    LC --> AuthSvc
    LC --> LoginSvc
    UC --> AuthSvc
    UC --> LoginSvc
    UC --> AppUserSvc
    EC --> EmgSvc

    %% Service → Service
    AuthSvc --> AuditSvc
    AppUserSvc --> AuditSvc
    LoginSvc --> AuditSvc

    %% Service → Repo
    AuthSvc --> UR
    AuthSvc --> RegR
    AppUserSvc --> UR
    LoginSvc --> UR
    LoginSvc --> RegR
    EmgSvc --> ECR
    AuditSvc --> AuditR

    %% Scheduler
    Sched -->|daily cron| UR
    Sched -->|triggers SMS alerts| EmgSvc

    %% Repo → DB
    UR --> DB
    RegR --> DB
    ECR --> DB
    AuditR --> DB

    %% Models map to DB (logical)
    DB -.->|Flyway migrations| UserM
    DB -.->|Flyway migrations| RegM
    DB -.->|Flyway migrations| ECM
    DB -.->|Flyway migrations| AuditM
```

---

## Package Structure

```
com.checkin
├── CheckinApplication            ← Spring Boot entry point
├── audit/
│   └── AuditAction                ← Enum (LOGIN, UPDATE_DETAILS, …)
├── config/
│   ├── EmergencyContactProperties ← @ConfigurationProperties
│   ├── JwtProperties              ← JWT secret / expiry
│   └── SecurityConfig             ← Filter chain, BCrypt, CORS
├── controller/
│   ├── AppUserController          ← /api/user/*
│   ├── AuthController             ← /api/auth/*
│   ├── EmergencyContactController ← /api/emergency-contacts/*
│   └── LoginController            ← /api/login/*
├── dto/                           ← Request/Response POJOs (13 DTOs)
├── model/
│   ├── AuditEvent
│   ├── EmergencyContact
│   ├── Registration
│   └── User
├── repository/                    ← JpaRepository interfaces (4)
├── scheduler/
│   └── InactiveUserScheduler      ← Daily inactive-user check
├── security/
│   ├── JwtAuthenticationFilter
│   ├── JwtService
│   └── UserPrincipal
└── service/
    ├── AppUserService
    ├── AuditService
    ├── AuthService
    ├── EmergencyContactService
    └── LoginService
```

---

## Key Flows

### 1. Registration
`POST /api/auth/register` → `AuthService.register` → saves `User` + `Registration` → returns `UserResponse`

### 2. Login
`POST /api/auth/login` → `AuthService.login` → validates password → records `AuditEvent(LOGIN)` → returns JWT in `AuthResponse`

### 3. Authenticated Request
`Bearer <token>` → `JwtAuthenticationFilter` → `JwtService.parseAndValidate` → populates `SecurityContext` with `UserPrincipal` → controller extracts `userId`

### 4. Password Reset
`POST /forgot-password` → `LoginService.forgotPassword` → stores hashed reset token on `User` → returns token  
`POST /reset-password` → `LoginService.resetPassword` → validates token + expiry → updates `passwordHash`

### 5. Inactive-User Alerting (Scheduler)
Daily at midnight → `InactiveUserScheduler` → queries users with `lastLoginDate` older than N days → calls `EmergencyContactService.sendSmsToAllContacts` for each
