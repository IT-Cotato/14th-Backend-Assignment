import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import * as authApi from '../api/auth'
import type { MeResponse } from '../api/auth'
import { setUnauthorizedHandler } from '../api/client'
import { AuthContext } from './AuthContext'
import { clearToken, getToken, setToken } from './token'

export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate()
  const [user, setUser] = useState<MeResponse | null>(null)
  // 토큰이 있으면 내 정보를 불러올 때까지 초기화 중으로 본다
  const [initializing, setInitializing] = useState(() => getToken() !== null)

  useEffect(() => {
    setUnauthorizedHandler(() => {
      setUser(null)
      navigate('/login', { replace: true })
    })
    return () => setUnauthorizedHandler(null)
  }, [navigate])

  useEffect(() => {
    if (getToken() === null) return
    authApi
      .getMe()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setInitializing(false))
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    const { accessToken } = await authApi.login({ email, password })
    setToken(accessToken)
    try {
      setUser(await authApi.getMe())
    } catch (error) {
      clearToken()
      throw error
    }
  }, [])

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } catch {
      // 서버 응답과 관계없이 저장한 토큰은 지운다
    } finally {
      clearToken()
      setUser(null)
      navigate('/login', { replace: true })
    }
  }, [navigate])

  const value = useMemo(
    () => ({ user, initializing, login, logout }),
    [user, initializing, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
