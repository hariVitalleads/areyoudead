# Are You Dead – Frontend

React web application for the Are You Dead API.

## Setup

```bash
npm install
```

## Development

Start the dev server (proxies `/api` to `http://localhost:8080`):

```bash
npm run dev
```

Ensure the backend is running at `http://localhost:8080`.

## Build

```bash
npm run build
```

For production with a different API URL:

```bash
VITE_API_URL=https://api.example.com npm run build
```

## Routes

| Path | Auth | Description |
|------|------|-------------|
| `/` | No | Dashboard / home |
| `/login` | No | Sign in |
| `/register` | No | Create account |
| `/forgot-password` | No | Request password reset |
| `/reset-password` | No | Reset password with token |
| `/account` | Yes | View/edit account details |
| `/emergency-contacts` | Yes | Manage emergency contacts |
