import type { ReactNode } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

export default function Layout({ children }: { children: ReactNode }) {
  const { token, user, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login');
  }

  return (
    <div className="layout">
      <header>
        <Link to="/" className="logo">Checkin</Link>
        {token && (
          <nav>
            <Link to="/">Dashboard</Link>
            <Link to="/account">Account</Link>
            <Link to="/emergency-contacts">Contacts</Link>
            <Link to="/activity">Check-in summary</Link>
            <span className="user">{user?.email}</span>
            <button onClick={handleLogout}>Sign out</button>
          </nav>
        )}
      </header>
      <main>{children}</main>
    </div>
  );
}
