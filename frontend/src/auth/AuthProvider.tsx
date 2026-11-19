import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'

import { getMe, login as loginApi, logout as logoutApi } from '../api/auth'
import { tokenStorage } from '../api/client'
import type { User } from '../api/types'
import { AuthContext, type AuthContextValue } from './authContext'

// 토큰은 localStorage에 저장해서 새로고침해도 로그인이 유지돼요.
// 토큰이 바뀔 때마다 /api/users/me로 내 정보를 다시 불러와요.
export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => tokenStorage.get())
  const [user, setUser] = useState<User | null>(null)
  // 내 정보를 불러온 토큰. token과 다르면 아직 불러오는 중이에요.
  const [loadedToken, setLoadedToken] = useState<string | null>(null)

  useEffect(() => {
    if (!token) {
      return
    }
    let cancelled = false
    getMe()
      .then((me) => {
        if (!cancelled) {
          setUser(me)
          setLoadedToken(token)
        }
      })
      .catch(() => {
        // 토큰이 무효면 지워요. (토큰을 보낸 401은 client.ts가 로그인 화면으로 보내요)
        if (!cancelled) {
          tokenStorage.clear()
          setToken(null)
          setUser(null)
        }
      })
    return () => {
      cancelled = true
    }
  }, [token])

  const login = useCallback(async (email: string, password: string) => {
    const { accessToken } = await loginApi({ email, password })
    tokenStorage.set(accessToken)
    setToken(accessToken)
  }, [])

  const logout = useCallback(async () => {
    try {
      await logoutApi()
    } finally {
      // 서버 요청이 실패해도 이 브라우저에서는 로그아웃 상태로 만들어요
      tokenStorage.clear()
      setToken(null)
      setUser(null)
      setLoadedToken(null)
    }
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      user: token ? user : null,
      isLoggedIn: token !== null,
      isLoading: token !== null && loadedToken !== token,
      login,
      logout,
    }),
    [token, user, loadedToken, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
