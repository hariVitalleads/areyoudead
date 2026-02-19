import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { resetPassword } from '../api';

export default function ResetPassword() {
  const [searchParams] = useSearchParams();
  const tokenFromUrl = searchParams.get('token');
  const [token, setToken] = useState(tokenFromUrl || '');
  const [newPassword, setNewPassword] = useState('');
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await resetPassword({ token, newPassword });
      setSuccess(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Reset failed');
    } finally {
      setLoading(false);
    }
  }

  if (success) {
    return (
      <div className="auth-page">
        <div className="auth-card">
          <h1>Password updated</h1>
          <p className="subtitle">You can now sign in with your new password.</p>
          <Link to="/login" className="btn">
            Sign in
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1>Reset password</h1>
        <p className="subtitle">Enter your token and new password</p>
        <form onSubmit={handleSubmit}>
          {error && <div className="error">{error}</div>}
          <input
            type="text"
            placeholder="Reset token"
            value={token}
            onChange={(e) => setToken(e.target.value)}
            required
          />
          <input
            type="password"
            placeholder="New password (min 8 characters)"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            minLength={8}
            required
          />
          <button type="submit" disabled={loading}>
            {loading ? 'Resetting…' : 'Reset password'}
          </button>
        </form>
        <p className="links">
          <Link to="/login">Back to sign in</Link>
        </p>
      </div>
    </div>
  );
}
