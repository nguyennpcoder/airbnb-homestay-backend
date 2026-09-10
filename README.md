# Airbnb Homestay - Backend

Spring Boot 3.5 + Java 21 + PostgreSQL backend for an Airbnb-style homestay booking platform.

## Tech stack

- Spring Boot 3.5.7, Java 21
- Spring Data JPA + Hibernate 6
- Spring Security + JWT
- PostgreSQL (Supabase)
- VNPay / ZaloPay / MoMo / Sepay integrations
- Twilio (SMS), Gmail SMTP (mail)

## Local development

```bash
# Copy env template and fill in values
cp .env.example .env

# Run
./mvnw spring-boot:run
```

## Build & run with Docker

```bash
docker build -t airbnb-backend .
docker run -p 8089:8089 --env-file .env airbnb-backend
```

## Deploy to Render

This repo includes a `Dockerfile` ready for Render Web Service.
Set environment variables in the Render dashboard (see `application.yml` for the full list).
