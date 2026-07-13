/**
 * @file Authentication provider and hook.
 *
 * Manages the entire auth lifecycle: persisting tokens in localStorage,
 * hydrating user state on mount, and exposing login/signup/logout to the
 * component tree via React Context.
 */

'use client';

import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { login as apiLogin, signup as apiSignup } from '@/lib/api/auth';
import type { AuthRequest, SignUpRequest } from '@/lib/api/types';

/** User profile stored in auth state */
interface User {
  email: string;
  name: string;
  profilePic?: string;
}

interface AuthContextValue {
  user: User | null;
  token: string | null;
  login: (data: AuthRequest) => Promise<void>;
  signup: (data: SignUpRequest) => Promise<void>;
  updateUser: (name: string, profilePic?: string) => void;
  logout: () => void;
  isAuthenticated: boolean;
  isLoading: boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

const TOKEN_KEY = 'spentra-token';
const USER_KEY = 'spentra-user';

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  /* Hydrate auth state from localStorage on mount */
  useEffect(() => {
    try {
      const storedToken = localStorage.getItem(TOKEN_KEY);
      const storedUser = localStorage.getItem(USER_KEY);

      if (storedToken && storedUser) {
        setToken(storedToken);
        setUser(JSON.parse(storedUser));
      }
    } catch {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const persistAuth = useCallback((authToken: string, authUser: User) => {
    localStorage.setItem(TOKEN_KEY, authToken);
    localStorage.setItem(USER_KEY, JSON.stringify(authUser));
    setToken(authToken);
    setUser(authUser);
  }, []);

  const login = useCallback(async (data: AuthRequest) => {
    const response = await apiLogin(data);
    persistAuth(response.token, {
      email: response.email,
      name: response.name,
      profilePic: response.profilePic,
    });
  }, [persistAuth]);

  const signup = useCallback(async (data: SignUpRequest) => {
    const response = await apiSignup(data);
    persistAuth(response.token, {
      email: response.email,
      name: response.name,
      profilePic: response.profilePic,
    });
  }, [persistAuth]);

  const updateUser = useCallback((name: string, profilePic?: string) => {
    setUser((prev) => {
      if (!prev) return null;
      const updated = { ...prev, name, profilePic };
      localStorage.setItem(USER_KEY, JSON.stringify(updated));
      return updated;
    });
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setToken(null);
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        login,
        signup,
        updateUser,
        logout,
        isAuthenticated: !!token,
        isLoading,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

/**
 * Access authentication state and actions from any client component.
 * @throws If used outside of an `<AuthProvider>`.
 */
export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
