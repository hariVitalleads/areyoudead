# Checkin – Frontend

React web application for the Checkin API.

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

### Firebase Authentication (optional)

To enable Google, Facebook, and Firebase email/password on the login/register pages, set:

- `VITE_FIREBASE_API_KEY`
- `VITE_FIREBASE_AUTH_DOMAIN` (e.g. `your-project.firebaseapp.com`)
- `VITE_FIREBASE_PROJECT_ID`

Create a web app in the [Firebase console](https://console.firebase.google.com/), enable Authentication providers, and add `http://localhost:5173` (or your dev URL) to authorized domains. The backend must have Firebase Admin credentials configured; see [docs/FIREBASE.md](../docs/FIREBASE.md).

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
