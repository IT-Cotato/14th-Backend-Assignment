import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import * as authApi from '../api/auth'
import { setUnauthorizedHandler } from '../api/client'
import type { User } from '../api/types'
import { AuthContext, type AuthStatus } from './authContext'
import { tokenStorage } from './tokenStorage'

export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate()
  const [user, setUser] = useState<User | null>(null)
  // 저장된 토큰이 있으면 내 정보를 불러올 때까지 loading이에요
  const [status, setStatus] = useState<AuthStatus>(() =>
    tokenStorage.get() ? 'loading' : 'anonymous',
  )

  // 401을 받으면 API 클라이언트가 토큰을 지운 뒤 이 함수를 불러요
  useEffect(() => {
    setUnauthorizedHandler(() => {
      setUser(null)
      setStatus('anonymous')
      navigate('/login', { replace: true })
    })
    return () => setUnauthorizedHandler(null)
  }, [navigate])

  // 새로고침해도 로그인이 유지되도록, 토큰이 있으면 내 정보를 다시 불러와요 (헤더 닉네임 표시용)
  useEffect(() => {
    if (!tokenStorage.get()) return
    let cancelled = false
    authApi
      .getMe()
      .then((me) => {
        if (cancelled) return
        setUser(me)
        setStatus('authenticated')
      })
      .catch(() => {
        if (cancelled) return
        tokenStorage.clear()
        setUser(null)
        setStatus('anonymous')
      })
    return () => {
      cancelled = true
    }
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    const { accessToken } = await authApi.login({ email, password })
    tokenStorage.set(accessToken)
    try {
      const me = await authApi.getMe()
      setUser(me)
      setStatus('authenticated')
    } catch (error) {
      tokenStorage.clear()
      throw error
    }
  }, [])

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } catch {
      // 서버 요청이 실패해도 이 브라우저에서는 로그아웃시켜요
    }
    tokenStorage.clear()
    setUser(null)
    setStatus('anonymous')
    navigate('/login', { replace: true })
  }, [navigate])

  const value = useMemo(() => ({ user, status, login, logout }), [user, status, login, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
