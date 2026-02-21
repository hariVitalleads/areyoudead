import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

export default function Dashboard() {
  const { user, token } = useAuth();

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
            <div className="nav-links">
              <Link to="/account">Account details</Link>
              <Link to="/emergency-contacts">Emergency contacts</Link>
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
