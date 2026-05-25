# Shoppio

Shoppio is a full-stack ecommerce application built to demonstrate production-style product thinking, not just isolated CRUD screens. It combines a polished customer storefront, a secure admin workspace, and a Spring Boot API that handles authentication, catalog management, cart flows, checkout, orders, wishlist management, and operational tooling.

This project is designed to show the kind of work a client, recruiter, or hiring team would expect from a developer who can build across the stack and think beyond the happy path.

## Why This Project Stands Out

- End-to-end ecommerce workflow from browsing products to placing orders
- Role-based admin system for managing products, orders, and users
- JWT-based authentication with protected routes and admin-only APIs
- Stripe checkout and webhook support for real payment integration patterns
- PostgreSQL persistence with Flyway migrations for maintainable schema evolution
- Redis caching and Actuator health endpoints for more realistic backend operations
- Frontend and backend split cleanly into separate apps with a clear local dev workflow

## What It Demonstrates

- Building a modern React frontend and a Spring Boot backend that work together cleanly
- Designing APIs for both customer-facing and admin-facing experiences
- Implementing security, validation, and protected access in a practical business app
- Structuring a repo so it is easy to develop, test, deploy, and extend
- Thinking about reliability through health checks, migrations, caching, and test coverage

## Core Product Areas

### Customer Experience

- Landing page and storefront browsing
- Product details flow
- Registration, login, and email verification
- Cart management and checkout
- Order history and order details
- Wishlist support

### Admin Experience

- Dashboard with revenue and operational metrics
- Product management
- Order management and status updates
- User management with role and status controls

### Platform and Backend Capabilities

- JWT authentication and Spring Security authorization rules
- Swagger / OpenAPI docs for API discoverability
- Flyway migrations for schema management
- Redis-backed caching
- Stripe webhook processing
- Actuator health and readiness endpoints

## Tech Stack

### Frontend

- React 19
- TypeScript
- Vite
- Tailwind CSS
- TanStack Query
- React Router

### Backend

- Java 17
- Spring Boot 3
- Spring Security
- Spring Data JPA
- PostgreSQL
- Redis
- Flyway
- Stripe
- Springdoc OpenAPI
- Actuator

### Testing and Tooling

- JUnit and Spring Boot Test
- Testcontainers
- Vitest
- ESLint
- Docker Compose

## Project Structure

```text
Shoppio/
|- backend/   Spring Boot API
|- frontend/  React + Vite client
\- docker-compose.yaml
```

- `backend` contains the API, database integration, security, business logic, and migrations
- `frontend` contains the storefront and admin UI
- `docker-compose.yaml` provides a quick local setup for PostgreSQL and Redis

More detailed service-level documentation:

- [backend/README.md](backend/README.md)
- [frontend/README.md](frontend/README.md)

## Engineering Highlights

- Security is enforced with stateless JWT authentication and role-based access for admin endpoints
- API documentation is available through Swagger UI and OpenAPI output
- Database schema changes are versioned under `backend/src/main/resources/db/migration`
- Health, readiness, and observability endpoints are exposed through Spring Actuator
- Backend tests cover controllers, services, Redis configuration, Stripe webhooks, and repository integration
- The frontend uses a separate Vite app with local API proxying for a cleaner development experience

## Local Development

### Prerequisites

- Java 17
- Node.js 18+ and npm
- Docker Desktop or local PostgreSQL and Redis

### 1. Start infrastructure

From the repository root:

```bash
docker compose up -d postgres redis
```

This starts:

- PostgreSQL on `localhost:5432`
- Redis on `localhost:6379`

### 2. Configure the backend

Create the backend environment file from the example:

```bash
cp backend/.env.example backend/.env
```

Review these required values:

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

Windows PowerShell:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The API runs on `http://localhost:8080`.

### 3. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend runs on `http://localhost:5173`.

During development, Vite proxies `/api` and `/images` to the backend on port `8080`.

## Useful URLs

- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI spec: `http://localhost:8080/v3/api-docs`
- Health check: `http://localhost:8080/actuator/health`

## Quality Checks

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

- The backend includes a production Dockerfile in `backend/Dockerfile`
- PostgreSQL schema changes are managed with Flyway migrations
- Redis, Stripe, mail, and image upload behavior depend on environment configuration
- The repository is structured so frontend and backend can be deployed independently

## License

No license file is currently included in this repository.
