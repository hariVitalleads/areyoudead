import { useState, useEffect } from 'react';
import {
  getCheckInSummary,
  getAuditEvents,
  checkIn,
} from '../api';
import type { CheckInSummaryResponse, AuditEventResponse } from '../types';

function formatDate(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleDateString(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  });
}

function actionLabel(action: string): string {
  const labels: Record<string, string> = {
    CHECK_IN: 'Check-in',
    LOGIN: 'Login',
    UPDATE_DETAILS: 'Updated details',
    PASSWORD_RESET_REQUESTED: 'Password reset requested',
    PASSWORD_RESET_COMPLETED: 'Password reset completed',
    ADMIN_VIEWED_USER: 'Admin viewed',
  };
  return labels[action] ?? action.replace(/_/g, ' ').toLowerCase();
}

export default function Activity() {
  const [summary, setSummary] = useState<CheckInSummaryResponse | null>(null);
  const [events, setEvents] = useState<AuditEventResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [checkingIn, setCheckingIn] = useState(false);
  const [showDetails, setShowDetails] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  function load() {
    setLoading(true);
    setError('');
    Promise.all([getCheckInSummary(), getAuditEvents()])
      .then(([s, e]) => {
        setSummary(s);
        setEvents(e);
      })
      .catch((err) => setError(err instanceof Error ? err.message : 'Load failed'))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    load();
  }, []);

  async function handleCheckIn() {
    setError('');
    setSuccess('');
    setCheckingIn(true);
    try {
      const res = await checkIn();
      setSuccess(res.message);
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Check-in failed');
    } finally {
      setCheckingIn(false);
    }
  }

  if (loading) return <div className="page">Loading…</div>;
  if (!summary) return <div className="page">Could not load activity.</div>;

  return (
    <div className="page">
      <div className="card">
        <h1>Check-in summary</h1>
        {error && <div className="error">{error}</div>}
        {success && <div className="success">{success}</div>}

        <div className="activity-summary">
          <div className="summary-item">
            <span className="summary-value">{summary.totalCheckIns}</span>
            <span className="summary-label">Total check-ins</span>
          </div>
          <div className="summary-item">
            <span className="summary-value">{summary.checkInsLast7Days}</span>
            <span className="summary-label">Last 7 days</span>
          </div>
          <div className="summary-item">
            <span className="summary-value">{summary.checkInsLast30Days}</span>
            <span className="summary-label">Last 30 days</span>
          </div>
          <div className="summary-item">
            <span className="summary-value">
              {summary.lastCheckInAt
                ? formatDate(summary.lastCheckInAt)
                : 'Never'}
            </span>
            <span className="summary-label">Last check-in</span>
          </div>
        </div>

        <button
          type="button"
          className="primary"
          onClick={handleCheckIn}
          disabled={checkingIn}
        >
          {checkingIn ? 'Recording…' : "I'm okay — Check in"}
        </button>
      </div>

      <div className="card">
        <h2>
          Activity log{' '}
          <button
            type="button"
            className="text-link"
            onClick={() => setShowDetails((v) => !v)}
          >
            {showDetails ? 'Hide' : 'Show'} details
          </button>
        </h2>

        {showDetails ? (
          events.length === 0 ? (
            <p className="muted">No activity recorded yet.</p>
          ) : (
            <ul className="activity-list">
              {events.map((e) => (
                <li key={e.id} className="activity-item">
                  <span className="activity-action">
                    {actionLabel(e.action)}
                    {e.details && (
                      <span className="activity-details"> — {e.details}</span>
                    )}
                  </span>
                  <span className="activity-date">{formatDate(e.createdAt)}</span>
                </li>
              ))}
            </ul>
          )
        ) : (
          <p className="muted">
            {events.length} event{events.length !== 1 ? 's' : ''} recorded.
          </p>
        )}
      </div>
    </div>
  );
}
