# AreYouDead (Spring Boot + JWT + Postgres)

Java + Spring Boot (Gradle) API with JWT authentication and Flyway-managed Postgres schema.

## Configuration

Set these environment variables (recommended) or edit `src/main/resources/application.yml`:

- `SPRING_DATASOURCE_URL` (default: `jdbc:postgresql://localhost:5432/areyoudead`)
- `SPRING_DATASOURCE_USERNAME` (default: `areyoudead`)
- `SPRING_DATASOURCE_PASSWORD` (default: `areyoudead`)
- `JWT_SECRET` (**required for real deployments**): HS256 secret, **>= 32 bytes**
- `JWT_ISSUER` (default: `areyoudead`)
- `JWT_TTL_SECONDS` (default: `3600`)

## Run

Start Postgres (optional helper):

```bash
docker compose up -d
```

With a local Gradle installation:

```bash
gradle bootRun
```

## API

### Register

`POST /api/auth/register`

Body:

```json
{ "email": "user@example.com", "password": "password123" }
```

Returns `201` with `{ tokenType, accessToken, user }`.

### Login

`POST /api/auth/login`

Body:

```json
{ "email": "user@example.com", "password": "password123" }
```

Returns `200` with `{ tokenType, accessToken, user }`.

### Me (JWT-protected example)

`GET /api/me` with header `Authorization: Bearer <token>`

## Flyway / Postgres

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

