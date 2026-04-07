import { initializeApp, type FirebaseApp } from 'firebase/app';
import { getAuth, type Auth } from 'firebase/auth';

const apiKey = import.meta.env.VITE_FIREBASE_API_KEY as string | undefined;
const authDomain = import.meta.env.VITE_FIREBASE_AUTH_DOMAIN as string | undefined;
const projectId = import.meta.env.VITE_FIREBASE_PROJECT_ID as string | undefined;

export function isFirebaseConfigured(): boolean {
  return Boolean(apiKey && authDomain && projectId);
}

let firebaseApp: FirebaseApp | null = null;
let firebaseAuth: Auth | null = null;

if (isFirebaseConfigured()) {
  firebaseApp = initializeApp({
    apiKey: apiKey!,
    authDomain: authDomain!,
    projectId: projectId!,
  });
  firebaseAuth = getAuth(firebaseApp);
}

export { firebaseApp, firebaseAuth };
