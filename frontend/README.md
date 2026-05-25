# Shoppio Frontend

React + TypeScript + Vite storefront and admin dashboard for the Shoppio ecommerce project.

## Stack

- React 19
- TypeScript
- Vite
- Tailwind CSS
- TanStack Query
- React Router

## Main Areas

- Customer storefront
- Product details
- Cart and checkout
- Orders and wishlist
- Admin dashboard
- Admin products, orders, and users

## Local Development

```bash
npm install
npm run dev
```

By default the frontend proxies `/api` and `/images` to `http://localhost:8080`.

## Build

```bash
npx tsc -b
npm run build
```

## Configuration

- `VITE_API_BASE`

Use `VITE_API_BASE` when the frontend is deployed separately from the backend.
