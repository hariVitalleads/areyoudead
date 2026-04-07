# Firebase Authentication

The SPA can sign in with **Google**, **Facebook**, or **email/password** through [Firebase Authentication](https://firebase.google.com/docs/auth). After Firebase signs the user in, the app sends the Firebase **ID token** to the backend; the server verifies it with the **Firebase Admin SDK** and issues the same JWT access/refresh pair as email/password login.

## Server configuration

Set **one** of:

| Variable | Description |
|----------|-------------|
| `FIREBASE_CREDENTIALS_JSON_BASE64` | Base64-encoded Firebase **service account** JSON (recommended in containers). |
| `FIREBASE_CREDENTIALS_PATH` | Filesystem path to the service account JSON file. |

If neither is set, `POST /api/user/auth/firebase` returns **503**.

In [application.yml](../src/main/resources/application.yml) these map to `app.firebase.credentials-json-base64` and `app.firebase.credentials-path`.

## Firebase console

1. Create a Firebase project and add a **Web** app to obtain the client config (`apiKey`, `authDomain`, `projectId`, etc.).
2. Enable **Authentication** sign-in methods: Email/Password, Google, Facebook (configure Facebook app id/secret in Firebase as required).
3. Create a **service account** with Firebase Admin privileges: Project settings → Service accounts → Generate new private key. Use that JSON for the backend (never commit it).

## Frontend environment variables

Configure the Vite app (public values only):

| Variable | Description |
|----------|-------------|
| `VITE_FIREBASE_API_KEY` | Web API key from Firebase console |
| `VITE_FIREBASE_AUTH_DOMAIN` | e.g. `your-project.firebaseapp.com` |
| `VITE_FIREBASE_PROJECT_ID` | GCP project id |

If these are unset, the UI hides Firebase sign-in and only shows classic email/password against this API.

Add authorized domains under Authentication → Settings (e.g. `localhost`, production host).

## Account conflicts

If an email is already registered via **classic** `POST /api/user/register` (bcrypt password), Firebase sign-in with the same email returns **409** until the user signs in with email/password or you implement account linking.
