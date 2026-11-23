import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useNavigate } from 'react-router'
import * as authApi from '../api/auth'
import { tokenStorage, UNAUTHORIZED_EVENT } from '../api/client'
import type { LoginRequest } from '../types'
import { AuthContext, type AuthContextValue } from './AuthContext'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => tokenStorage.get())
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  const { data: user } = useQuery({
    queryKey: ['me', token],
    queryFn: authApi.fetchMe,
    enabled: token !== null,
  })

  // 토큰이 만료되거나 무효가 되어 401 이 오면 로그인 화면으로 보낸다
  useEffect(() => {
    const handleUnauthorized = () => {
      setToken(null)
      queryClient.clear()
      navigate('/login', { replace: true })
    }
    window.addEventListener(UNAUTHORIZED_EVENT, handleUnauthorized)
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, handleUnauthorized)
  }, [navigate, queryClient])

  const login = useCallback(
    async (request: LoginRequest) => {
      const { accessToken } = await authApi.login(request)
      tokenStorage.set(accessToken)
      setToken(accessToken)
      // isAuthor 처럼 로그인 여부에 따라 달라지는 값을 다시 받아온다
      await queryClient.invalidateQueries()
    },
    [queryClient],
  )

  const logout = useCallback(async () => {
    // 서버 요청이 실패해도 저장해 둔 토큰은 지우고 로그인 화면으로 이동한다
    await authApi.logout().catch(() => undefined)
    tokenStorage.clear()
    setToken(null)
    queryClient.clear()
    navigate('/login', { replace: true })
  }, [navigate, queryClient])

  const value = useMemo<AuthContextValue>(
    () => ({ isLoggedIn: token !== null, user: user ?? null, login, logout }),
    [token, user, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
