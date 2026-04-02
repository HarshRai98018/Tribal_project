# TribalCraft Connect

Full-stack tribal marketplace app with:
- Spring Boot backend (auth, products, orders, payments, reviews, issues)
- Static React frontend (served by Spring Boot)
- PostgreSQL (recommended: Supabase Postgres)
- Supabase Storage for product image uploads

## Tech Stack

- Java 21
- Spring Boot 3.4
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL / H2 (local fallback)
- React 18 (CDN + JSX in `frontend/`)

## Project Structure

```text
.
|-- src/main/java                 # Backend code
|-- src/main/resources            # Backend config
|-- frontend/                     # Static React files (served at runtime)
|-- pom.xml
|-- Dockerfile
`-- render.yaml
```

## Local Run

### 1) Start backend + frontend together

```bash
mvn spring-boot:run
```

Open:
- `http://localhost:8080`

Frontend is served from `frontend/` by Spring, so no separate frontend server is required.

### 2) Local DB options

- If you **do not set DB env vars**, app uses local in-memory H2 for development.
- To use Supabase/Postgres, set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (see `.env.example`).

## Environment Variables

Copy `.env.example` and set values as environment variables before running:
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET`
- `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`, `SUPABASE_BUCKET_NAME` (for image upload)
- `RECAPTCHA_ENABLED`, `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY` (optional locally, recommended in prod)
- `CORS_ALLOWED_ORIGINS`

## Supabase Image Upload

Endpoint:
- `POST /api/products/{id}/upload-image` (multipart form field: `file`)

The backend uploads images to Supabase Storage bucket and saves public URL in product record.

Required env vars:
- `SUPABASE_URL`
- `SUPABASE_SERVICE_ROLE_KEY`
- `SUPABASE_BUCKET_NAME`

## Demo Credentials (Seeded)

- `admin@tribalcraft.com` / `Admin@123`
- `artisan@tribalcraft.com` / `Artisan@123`
- `customer@tribalcraft.com` / `Customer@123`
- `consultant@tribalcraft.com` / `Consultant@123`

## Notes

- Never commit real secrets in `application.yml`.
- For production, always set strong `JWT_SECRET` and enable CAPTCHA.
