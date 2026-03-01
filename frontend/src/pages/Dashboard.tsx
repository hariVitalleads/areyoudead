import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { getCheckInSummary } from '../api';
import type { CheckInSummaryResponse } from '../types';

export default function Dashboard() {
  const { user, token } = useAuth();
  const [summary, setSummary] = useState<CheckInSummaryResponse | null>(null);

  useEffect(() => {
    if (token) {
      getCheckInSummary()
        .then(setSummary)
        .catch(() => setSummary(null));
    }
  }, [token]);

  return (
    <div className="page">
      <div className="card hero">
        <h1>Checkin</h1>
        <p className="subtitle">
          Peace of mind for your loved ones. If you become inactive, we notify
          your emergency contacts.
        </p>
        {token && user ? (
          <>
            <p className="welcome">Welcome, {user.email}</p>
            {summary != null && (
              <div className="dashboard-summary">
                <p className="summary-headline">
                  Check-in summary: <strong>{summary.totalCheckIns}</strong> total
                  {summary.lastCheckInAt && (
                    <> · Last: {new Date(summary.lastCheckInAt).toLocaleDateString()}</>
                  )}
                </p>
                <Link to="/activity" className="summary-link">
                  View full summary & activity →
                </Link>
              </div>
            )}
            <div className="nav-links">
              <Link to="/account">Account details</Link>
              <Link to="/emergency-contacts">Emergency contacts</Link>
              <Link to="/activity">Check-in summary</Link>
            </div>
          </>
        ) : (
          <div className="nav-links">
            <Link to="/login">Sign in</Link>
            <Link to="/register">Create account</Link>
          </div>
        )}
      </div>
    </div>
  );
}
