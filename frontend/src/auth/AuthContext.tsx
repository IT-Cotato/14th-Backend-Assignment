import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import * as authApi from '../api/auth'
import { tokenStorage } from '../api/client'
import type { MyInfo } from '../api/types'

interface AuthState {
  isLoggedIn: boolean
  me: MyInfo | null
  login: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
  refreshMe: () => Promise<void>
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => tokenStorage.get())
  const [me, setMe] = useState<MyInfo | null>(null)

  const refreshMe = useCallback(async () => {
    if (!tokenStorage.get()) {
      setMe(null)
      return
    }
    try {
      setMe(await authApi.getMyInfo())
    } catch {
      setMe(null)
    }
  }, [])

  useEffect(() => {
    void refreshMe()
  }, [token, refreshMe])

  const login = useCallback(async (email: string, password: string) => {
    const { accessToken } = await authApi.login(email, password)
    tokenStorage.set(accessToken)
    setToken(accessToken)
  }, [])

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } finally {
      tokenStorage.clear()
      setToken(null)
      setMe(null)
    }
  }, [])

  const value = useMemo(
    () => ({ isLoggedIn: Boolean(token), me, login, logout, refreshMe }),
    [token, me, login, logout, refreshMe],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth는 AuthProvider 안에서만 쓸 수 있어요.')
  return ctx
}
