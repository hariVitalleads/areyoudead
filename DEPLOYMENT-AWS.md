# AWS Container Deployment Guide

This guide covers deploying the Checkin API as a container on AWS (ECS/Fargate, ECR, RDS, SES).

## Prerequisites

- AWS CLI configured
- Docker installed
- RDS PostgreSQL instance (or use local Postgres for testing)

## 1. Build and Run Locally

```bash
# Build image
docker build -t checkin-api:latest .

# Run with env file (create .env from .env.example)
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/areyoudead?options=-c%20TimeZone%3DUTC \
  -e SPRING_DATASOURCE_USERNAME=areyoudead \
  -e SPRING_DATASOURCE_PASSWORD=areyoudead \
  -e JWT_SECRET=your-32-byte-secret-minimum-required \
  checkin-api:latest
```

Or use docker-compose:

```bash
# Create .env with JWT_SECRET (required for prod profile)
echo "JWT_SECRET=your-32-byte-secret-minimum-required" > .env

docker compose -f docker-compose.prod.yml up -d
```

Health check: `http://localhost:8080/checkin/actuator/health`

## 2. Push Image to Amazon ECR

```bash
# Create ECR repository (one-time)
aws ecr create-repository --repository-name checkin-api

# Login to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com

# Tag and push
docker build -t checkin-api:latest .
docker tag checkin-api:latest <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/checkin-api:latest
docker push <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/checkin-api:latest
```

## 3. RDS PostgreSQL Setup

1. Create an RDS PostgreSQL 16 instance (or 15+).
2. Ensure the security group allows inbound from your ECS tasks (port 5432).
3. Create database `areyoudead` and user/password.
4. Run Flyway migrations before first app deploy:

```bash
# Using Flyway Docker
docker run --rm -v $(pwd)/src/main/resources/db/migration:/flyway/sql \
  flyway/flyway \
  -url="jdbc:postgresql://<RDS_ENDPOINT>:5432/areyoudead?options=-c%20TimeZone%3DUTC" \
  -user=<DB_USER> \
  -password=<DB_PASSWORD> \
  -connectRetries=60 \
  migrate
```

## 4. ECS Task Definition (Fargate)

Example task definition environment variables:

| Variable | Description | Required |
|----------|-------------|----------|
| `SPRING_DATASOURCE_URL` | RDS JDBC URL | Yes |
| `SPRING_DATASOURCE_USERNAME` | DB username | Yes |
| `SPRING_DATASOURCE_PASSWORD` | DB password (use Secrets Manager) | Yes |
| `JWT_SECRET` | HS256 secret, ≥32 bytes | Yes |
| `JWT_ISSUER` | Token issuer (e.g. `checkin`) | Yes |
| `APP_BASE_URL` | Public base URL (e.g. `https://api.example.com/checkin`) | Yes |
| `MAIL_HOST` | SMTP host (SES: `email-smtp.<region>.amazonaws.com`) | For email |
| `MAIL_PORT` | 587 (SES) | For email |
| `MAIL_USERNAME` | SES SMTP username | For email |
| `MAIL_PASSWORD` | SES SMTP password | For email |
| `MAIL_SMTP_AUTH` | `true` | For email |
| `MAIL_SMTP_STARTTLS` | `true` | For email |
| `SPRING_PROFILES_ACTIVE` | `prod` | Recommended |

### Amazon SES (Email)

1. Verify your domain/email in SES.
2. Create SMTP credentials in SES Console → SMTP settings.
3. If in SES sandbox, verify recipient emails for testing.
4. Set `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`.

## 5. Load Balancer Health Check

- **Path**: `/checkin/actuator/health`
- **Port**: 8080 (or mapped port)
- **Interval**: 30s
- **Timeout**: 10s
- **Healthy threshold**: 2
- **Unhealthy threshold**: 3

## 6. Frontend Deployment (Optional)

The frontend (Vite/React) is separate. For production:

1. Build: `cd frontend && npm run build`
2. Set `VITE_API_URL` to your API base (e.g. `https://api.example.com`) before build
3. Deploy `dist/` to S3 + CloudFront, or another static host

## 7. Quick Reference: Required Env Vars

```bash
# Minimum for production
SPRING_DATASOURCE_URL=jdbc:postgresql://<RDS_HOST>:5432/areyoudead?options=-c%20TimeZone%3DUTC
SPRING_DATASOURCE_USERNAME=areyoudead
SPRING_DATASOURCE_PASSWORD=<secret>
JWT_SECRET=<32+ bytes>
APP_BASE_URL=https://your-api.domain.com/checkin

# For email (SES)
MAIL_HOST=email-smtp.us-east-1.amazonaws.com
MAIL_PORT=587
MAIL_USERNAME=<ses-smtp-user>
MAIL_PASSWORD=<ses-smtp-password>
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
```
