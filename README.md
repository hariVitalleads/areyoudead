# Checkin (Spring Boot + JWT + Postgres)

Java + Spring Boot (Gradle) API with JWT authentication and Flyway-managed Postgres schema.

## Configuration

Set these environment variables (recommended) or edit `src/main/resources/application.yml`:

- `SPRING_DATASOURCE_URL` (default: `jdbc:postgresql://localhost:5432/areyoudead`)
- `SPRING_DATASOURCE_USERNAME` (default: `areyoudead`)
- `SPRING_DATASOURCE_PASSWORD` (default: `areyoudead`)
- `SPRING_DATASOURCE_URL` (default uses UTC; if overriding, append `?options=-c%20TimeZone%3DUTC`)
- `JWT_SECRET` (**required for real deployments**): HS256 secret, **>= 32 bytes**
- `JWT_ISSUER` (default: `areyoudead`)
- `JWT_TTL_SECONDS` (default: `3600`)

## Run

Start Postgres and MailHog (optional helpers):

```bash
docker compose up -d
```

This starts:
- **Postgres** on `localhost:5432`
- **MailHog** (fake SMTP) on `localhost:1025`, Web UI on **http://localhost:8025**

With a local Gradle installation:

```bash
gradle bootRun
```

### Viewing emails (MailHog)

1. Start MailHog: `docker compose up -d mail`
2. Open **http://localhost:8025** in your browser
3. Spring Boot sends emails to `localhost:1025` by default (see `MAIL_HOST`, `MAIL_PORT` in `application.yml`)
4. Emails appear when the inactive-user scheduler notifies emergency contacts (users inactive for `app.scheduler.inactive-ms`)

**To trigger an email:**
- Create a user, add emergency contacts (with email), and ensure the user has `last_login_date` older than `app.scheduler.inactive-ms` (default 1 day). The scheduler runs every 10 seconds.

## API

The canonical API is `/api/user`. Use these endpoints:

### Register

`POST /api/user/register`

Body:

```json
{ "email": "user@example.com", "password": "password123" }
```

Returns `201` with `{ id, email, createdAt }`.

### Login

`POST /api/user/login` (alias: `POST /api/login/login`)

Body:

```json
{ "email": "user@example.com", "password": "password123" }
```

Returns `200` with `{ tokenType, accessToken, user }`.

### Refresh

`POST /api/user/refresh`

Body:

```json
{ "refreshToken": "<refresh-token>" }
```

### Me (JWT-protected)

`GET /api/user/me` with header `Authorization: Bearer <token>`

## Flyway / Postgres

**Reset Flyway and all tables** (clears migration history and data):
```bash
psql -h localhost -U areyoudead -d areyoudead -f scripts/reset-flyway.sql
# Or with Docker Compose:
docker compose exec -T postgres psql -U areyoudead -d areyoudead < scripts/reset-flyway.sql
```

Schema is created by Flyway migration:

- `src/main/resources/db/migration/V1__init.sql`

Equivalent Postgres query used by the registration API (conceptually):

```sql
INSERT INTO app_user (email, password_hash, created_at)
VALUES ($1, $2, now())
RETURNING id, email, created_at;
```

Registration table insert (one-to-one with `app_user`):

```sql
INSERT INTO registration (user_id, registration_type, has_paid, paid_at)
VALUES ($1, $2, false, NULL);
```

