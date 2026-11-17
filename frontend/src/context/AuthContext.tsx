import { createContext, useContext, useEffect, useState } from "react";
import type { ReactNode } from "react";

import { clearToken, getStoredToken, storeToken } from "../api/client";
import { getMyInfo, logout as logoutApi } from "../api/auth";
import type { MyInfoResponse } from "../api/auth";

interface AuthContextValue {
  user: MyInfoResponse | null;
  isLoading: boolean;
  login: (token: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<MyInfoResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const token = getStoredToken();
    if (!token) {
      setIsLoading(false);
      return;
    }

    getMyInfo()
      .then(setUser)
      .catch(() => {
        clearToken();
        setUser(null);
      })
      .finally(() => setIsLoading(false));
  }, []);

  async function login(token: string) {
    storeToken(token);
    const me = await getMyInfo();
    setUser(me);
  }

  async function logout() {
    try {
      await logoutApi();
    } finally {
      clearToken();
      setUser(null);
    }
  }

  return <AuthContext.Provider value={{ user, isLoading, login, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
