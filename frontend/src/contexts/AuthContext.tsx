import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import type { AuthResponse, UserResponse } from '../types';

interface AuthContextValue {
  token: string | null;
  user: UserResponse | null;
  login: (res: AuthResponse) => void;
  logout: () => void;
  isReady: boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

const TOKEN_KEY = 'token';
const USER_KEY = 'user';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_KEY));
  const [user, setUser] = useState<UserResponse | null>(() => {
    const s = localStorage.getItem(USER_KEY);
    return s ? JSON.parse(s) : null;
  });
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    setIsReady(true);
  }, []);

  const login = (res: AuthResponse) => {
    setToken(res.accessToken);
    setUser(res.user);
    localStorage.setItem(TOKEN_KEY, res.accessToken);
    localStorage.setItem(USER_KEY, JSON.stringify(res.user));
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  };

  return (
    <AuthContext.Provider value={{ token, user, login, logout, isReady }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
