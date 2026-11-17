import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useNavigate } from 'react-router'
import { fetchMe, logout as logoutRequest } from '../api/auth'
import { setUnauthorizedHandler } from '../api/client'
import type { UserMe } from '../api/types'
import { AuthContext, type AuthContextValue } from './AuthContext'
import { tokenStorage } from './tokenStorage'

const SESSION_EXPIRED_MESSAGE = '로그인이 만료되었어요. 다시 로그인해 주세요.'

export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate()
  const [user, setUser] = useState<UserMe | null>(null)
  // 저장된 토큰이 있으면 내 정보를 불러올 때까지 로딩 상태로 시작한다.
  const [loading, setLoading] = useState<boolean>(() => tokenStorage.get() !== null)

  const clearSession = useCallback(() => {
    tokenStorage.clear()
    setUser(null)
  }, [])

  // 토큰이 만료·무효가 되어 401(COMMON-004)이 오면 상태를 정리하고 로그인 화면으로 보낸다.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      clearSession()
      navigate('/login', { replace: true, state: { message: SESSION_EXPIRED_MESSAGE } })
    })
    return () => setUnauthorizedHandler(null)
  }, [clearSession, navigate])

  // 새로고침 후에도 로그인이 유지되도록 저장된 토큰으로 내 정보를 복구한다.
  useEffect(() => {
    if (tokenStorage.get() === null) {
      return
    }
    let cancelled = false
    fetchMe()
      .then((me) => {
        if (!cancelled) setUser(me)
      })
      .catch(() => {
        if (!cancelled) clearSession()
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [clearSession])

  const login = useCallback(async (accessToken: string) => {
    tokenStorage.set(accessToken)
    try {
      setUser(await fetchMe())
    } catch (error) {
      tokenStorage.clear()
      throw error
    }
  }, [])

  const logout = useCallback(async () => {
    try {
      await logoutRequest()
    } catch {
      // 이미 만료된 토큰이라 서버 호출이 실패해도 화면의 로그인 상태는 정리한다.
    } finally {
      clearSession()
    }
  }, [clearSession])

  const value = useMemo<AuthContextValue>(
    () => ({ user, isAuthenticated: user !== null, loading, login, logout }),
    [user, loading, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
