import { useState, type FormEvent } from 'react';
import {
  type Auth,
  GoogleAuthProvider,
  FacebookAuthProvider,
  signInWithPopup,
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
} from 'firebase/auth';
import { firebaseAuth, isFirebaseConfigured } from '../firebase/client';
import { authenticateWithFirebase } from '../api';
import type { AuthResponse } from '../types';

type Mode = 'login' | 'register';

export default function FirebaseAuthPanel({
  mode,
  onSuccess,
}: {
  mode: Mode;
  onSuccess: (res: AuthResponse) => void;
}) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);

  if (!isFirebaseConfigured() || firebaseAuth === null) {
    return null;
  }
  const auth: Auth = firebaseAuth;

  async function completeWithIdToken() {
    const u = auth.currentUser;
    if (!u) {
      throw new Error('Not signed in');
    }
    const idToken = await u.getIdToken();
    const res = await authenticateWithFirebase(idToken);
    onSuccess(res);
  }

  async function google() {
    setErr('');
    setBusy(true);
    try {
      await signInWithPopup(auth, new GoogleAuthProvider());
      await completeWithIdToken();
    } catch (e) {
      setErr(e instanceof Error ? e.message : 'Google sign-in failed');
    } finally {
      setBusy(false);
    }
  }

  async function facebook() {
    setErr('');
    setBusy(true);
    try {
      await signInWithPopup(auth, new FacebookAuthProvider());
      await completeWithIdToken();
    } catch (e) {
      setErr(e instanceof Error ? e.message : 'Facebook sign-in failed');
    } finally {
      setBusy(false);
    }
  }

  async function handleEmail(e: FormEvent) {
    e.preventDefault();
    setErr('');
    setBusy(true);
    try {
      if (mode === 'login') {
        await signInWithEmailAndPassword(auth, email, password);
      } else {
        await createUserWithEmailAndPassword(auth, email, password);
      }
      await completeWithIdToken();
    } catch (e) {
      setErr(e instanceof Error ? e.message : 'Email sign-in failed');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="firebase-auth">
      <p className="firebase-auth-title">
        {mode === 'login' ? 'Sign in with Firebase' : 'Create account with Firebase'}
      </p>
      <div className="firebase-auth-buttons">
        <button
          type="button"
          className="btn-firebase btn-google"
          disabled={busy}
          onClick={() => void google()}
        >
          Google
        </button>
        <button
          type="button"
          className="btn-firebase btn-facebook"
          disabled={busy}
          onClick={() => void facebook()}
        >
          Facebook
        </button>
      </div>
      <form className="firebase-email-form" onSubmit={(ev) => void handleEmail(ev)}>
        <input
          type="email"
          autoComplete="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <input
          type="password"
          autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          minLength={6}
          required
        />
        <button type="submit" disabled={busy}>
          {busy
            ? '…'
            : mode === 'login'
              ? 'Sign in with email (Firebase)'
              : 'Create account with email (Firebase)'}
        </button>
      </form>
      {err && <div className="error">{err}</div>}
    </div>
  );
}
