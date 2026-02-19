import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

export default function ProtectedRoute({ children }: { children: ReactNode }) {
  const { token, isReady } = useAuth();
  const location = useLocation();

  if (!isReady) return <div className="page">Loading…</div>;
  if (!token) return <Navigate to="/login" state={{ from: location }} replace />;
  return <>{children}</>;
}
