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
| `SPRING_PROFILES_ACTIVE` | `prod` | **Recommended** – enables HikariCP tuning for containers |

### Amazon SES (Email)

1. Verify your domain/email in SES.
2. Create SMTP credentials in SES Console → SMTP settings.
3. If in SES sandbox, verify recipient emails for testing.
4. Set `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`.

## 5. ECS Task Size & Health Checks

**Thread starvation / app not starting:** If you see `HikariPool - Thread starvation or clock leap detected` and the app hangs, the task likely has too little CPU.

- **Minimum recommended:** 512 CPU units (0.5 vCPU), 1024 MB memory
- **Safer for cold start:** 1024 CPU units (1 vCPU), 2048 MB memory

**ECS health check grace period:** Set to at least 120–180 seconds so the app can finish Flyway + startup before health checks run.

## 6. Load Balancer Health Check

- **Path**: `/checkin/actuator/health`
- **Port**: 8080 (or mapped port)
- **Interval**: 30s
- **Timeout**: 10s
- **Healthy threshold**: 2
- **Unhealthy threshold**: 3

## 7. Frontend Deployment on AWS

The frontend is a Vite/React SPA. Two deployment options:

### Option A: S3 + CloudFront (recommended for static sites)

Low cost, scalable, and well-suited for SPAs.

**Step 1: Build the frontend**

```bash
cd frontend

# Use your API’s public URL (must be reachable from the browser)
VITE_API_URL=https://api.yourdomain.com/checkin npm run build

# Or if API is on an IP for testing:
# VITE_API_URL=https://feeling-okay.com/checkin npm run build
```

**Step 2: Create S3 bucket**

```bash
BUCKET_NAME=checkin-frontend  # Use a unique name
REGION=us-east-1

aws s3 mb s3://${BUCKET_NAME} --region ${REGION}

# Block public access (CloudFront will use OAI)
aws s3api put-public-access-block \
  --bucket ${BUCKET_NAME} \
  --public-access-block-configuration "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"
```

**Step 3: Upload build output**

```bash
aws s3 sync dist/ s3://${BUCKET_NAME} --delete
```

**Step 4: Create CloudFront distribution**

1. CloudFront → Create distribution
2. **Origin**: S3 bucket `s3://${BUCKET_NAME}`
3. **Origin access**: Origin access control (recommended) or legacy OAI
4. **Default root object**: `index.html`
5. **Error pages**: Add custom error response for 403 and 404 → return `200` and `/index.html` (for SPA routing)
6. Create distribution and note the CloudFront URL (e.g. `https://d1234.cloudfront.net`)

**Step 5: Set S3 bucket policy (OAC/OAI)**

After creating the distribution, apply the policy suggested by CloudFront so it can read from the bucket.

**Step 6: (Optional) Custom domain + HTTPS**

- Add an alternate domain name (e.g. `app.yourdomain.com`)
- Request or import an ACM certificate in `us-east-1`
- Update DNS to point to the CloudFront domain

---

### Option B: ECS (container, aligned with API)

Use the same pattern as the API: build image, push to ECR, run on ECS.

**Step 1: Build with correct API URL**

```bash
# Replace with your API public URL
VITE_API_URL=https://api.yourdomain.com/checkin docker build -t checkin-frontend:latest -f frontend/Dockerfile frontend/

# Or for IP-based testing:
VITE_API_URL=https://feeling-okay.com/checkin docker build -t checkin-frontend:latest -f frontend/Dockerfile frontend/
```

**Step 2: Create ECR repository and push**

```bash
aws ecr create-repository --repository-name checkin-frontend

ECR_URI=$(aws ecr describe-repositories --repository-names checkin-frontend --query 'repositories[0].repositoryUri' --output text)
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin $(echo $ECR_URI | cut -d/ -f1)

docker tag checkin-frontend:latest ${ECR_URI}:latest
docker push ${ECR_URI}:latest
```

**Step 3: ECS task definition**

- **Image**: `${ECR_URI}:latest`
- **CPU**: 256
- **Memory**: 512 MB
- **Port mappings**: 80
- **Health check**: `GET /` on port 80

**Step 4: ECS service**

- Create a service using the task definition
- Use an ALB target group with path `/` on port 80
- Set health check path to `/`
- Register the service with the ALB

**Step 5: ALB / DNS**

- Create or reuse an ALB listener for port 80/443
- Add a rule to forward traffic to the frontend target group
- Point your domain (e.g. `app.yourdomain.com`) to the ALB

## 8. Quick Reference: Required Env Vars

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

## Troubleshooting

**JavaMailSender bean not found / APPLICATION FAILED TO START:**
- The app now includes a fallback no-op `JavaMailSender` when `spring.mail.host` is empty, so it should start even without email configured.
- To enable real email: set `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` (e.g. for Amazon SES).
- If you see this error, ensure you're not excluding mail autoconfiguration and that `MAIL_HOST` is either unset (uses localhost) or a valid SMTP host.

**App hangs after "Thread starvation or clock leap detected":**
1. Increase ECS task CPU to at least 512 (0.5 vCPU), preferably 1024.
2. Ensure `SPRING_PROFILES_ACTIVE=prod` is set (tunes HikariCP for containers).
3. Extend ECS health check grace period to 120–180 seconds.
