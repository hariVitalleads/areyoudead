import { useState } from 'react';
import { Link } from 'react-router-dom';
import { forgotPassword } from '../api';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [sent, setSent] = useState(false);
  const [resetToken, setResetToken] = useState<string | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await forgotPassword(email);
      setSent(true);
      setResetToken(res.resetToken);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Request failed');
    } finally {
      setLoading(false);
    }
  }

  if (sent) {
    return (
      <div className="auth-page">
        <div className="auth-card">
          <h1>Check your email</h1>
          <p className="subtitle">
            If an account exists for {email}, a reset link has been issued.
          </p>
          {resetToken && (
            <div className="token-box">
              <p>Development: use this token on the reset page:</p>
              <code>{resetToken}</code>
            </div>
          )}
          <p className="links">
            <Link to="/login">Back to sign in</Link>
            <Link to="/reset-password">Reset password with token</Link>
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1>Forgot password</h1>
        <p className="subtitle">Enter your email to request a reset</p>
        <form onSubmit={handleSubmit}>
          {error && <div className="error">{error}</div>}
          <input
            type="email"
            placeholder="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <button type="submit" disabled={loading}>
            {loading ? 'Sending…' : 'Send reset link'}
          </button>
        </form>
        <p className="links">
          <Link to="/login">Back to sign in</Link>
        </p>
      </div>
    </div>
  );
}
