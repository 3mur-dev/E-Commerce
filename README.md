# Shoppio

Shoppio is a full-stack ecommerce project with a React storefront, an admin dashboard, and a Spring Boot API for authentication, catalog management, cart, checkout, orders, wishlist, and admin operations.

## Project Structure

```text
Shoppio/
|- backend/   Spring Boot API
|- frontend/  React + Vite client
\- docker-compose.yaml
```

- `backend`: Java 17, Spring Boot, PostgreSQL, Redis, Stripe, Flyway, Swagger, Actuator
- `frontend`: React, TypeScript, Vite, Tailwind CSS, TanStack Query, React Router

See the service-specific docs for more detail:

- [backend/README.md](backend/README.md)
- [frontend/README.md](frontend/README.md)

## Features

- Customer storefront with product browsing and details
- Authentication and email verification
- Cart, checkout, and order history
- Wishlist support
- Admin dashboard for products, orders, and users
- Stripe checkout and webhook handling
- Redis-backed caching and Flyway migrations

## Prerequisites

- Java 17
- Node.js 18+ and npm
- Docker Desktop or local PostgreSQL and Redis

## Local Development

### 1. Start infrastructure

If you want to use Docker for local services, start Postgres and Redis from the repository root:

```bash
docker compose up -d postgres redis
```

That provides:

- PostgreSQL on `localhost:5432`
- Redis on `localhost:6379`

### 2. Configure the backend

Use the example environment file in `backend`:

```bash
cp backend/.env.example backend/.env
```

Required values you should review before starting:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `CORS_ALLOWED_ORIGINS`
- `FRONTEND_URL`

Common optional integrations:

- `REDIS_HOST`
- `REDIS_PORT`
- `STRIPE_SECRET_KEY`
- `STRIPE_WEBHOOK_SECRET`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_FROM`
- `ADMIN_USERNAME`
- `ADMIN_EMAIL`
- `ADMIN_PASSWORD`
- `CLOUDINARY_URL`

Run the backend:

```bash
cd backend
./mvnw spring-boot:run
```

On Windows PowerShell, you can also use:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The API runs on `http://localhost:8080`.

### 3. Configure and run the frontend

Install dependencies and start Vite:

```bash
cd frontend
npm install
npm run dev
```

The frontend runs on `http://localhost:5173`.

During local development, Vite proxies `/api` and `/images` to the backend on port `8080`.

## Useful URLs

- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI spec: `http://localhost:8080/v3/api-docs`
- Health check: `http://localhost:8080/actuator/health`

## Testing and Quality Checks

Backend:

```bash
cd backend
./mvnw test
```

Frontend:

```bash
cd frontend
npm run lint
npm run test
npm run build
```

## Deployment Notes

- The backend includes a production `Dockerfile` in `backend/Dockerfile`.
- Database schema changes are managed with Flyway migrations under `backend/src/main/resources/db/migration`.
- Redis, Stripe, mail, and image upload integrations depend on environment configuration.

## License

No license file is currently included in this repository.
