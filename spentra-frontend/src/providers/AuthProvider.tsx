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
import { getProfile } from '@/lib/api/users';
import type { AuthRequest, SignUpRequest } from '@/lib/api/types';
import { SPENTRA_TOKEN_KEY, SPENTRA_USER_KEY } from '@/lib/constants/auth';

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

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  /* Hydrate auth state from localStorage on mount */
  useEffect(() => {
    async function hydrate() {
      try {
        const storedToken = localStorage.getItem(SPENTRA_TOKEN_KEY);

        if (storedToken) {
          setToken(storedToken);
          const profile = await getProfile();
          setUser({
            email: profile.email,
            name: profile.name,
            profilePic: profile.profilePic,
          });
        }
      } catch {
        // If profile fetch fails or token is invalid, clear token/user state and legacy cached data
        localStorage.removeItem(SPENTRA_TOKEN_KEY);
        localStorage.removeItem(SPENTRA_USER_KEY);
        setToken(null);
        setUser(null);
      } finally {
        setIsLoading(false);
      }
    }
    hydrate();
  }, []);

  const persistAuth = useCallback((authToken: string, authUser: User) => {
    localStorage.setItem(SPENTRA_TOKEN_KEY, authToken);
    // User PII is NOT stored in localStorage to prevent data leakage and exposure
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
      return { ...prev, name, profilePic };
    });
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem(SPENTRA_TOKEN_KEY);
    localStorage.removeItem(SPENTRA_USER_KEY); // Clean up legacy user data if any
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
