import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { getMyInfo, logout as logoutApi } from '../api/auth'
import { setUnauthorizedHandler } from '../api/client'
import { AuthContext, ME_QUERY_KEY, type AuthContextValue } from './AuthContext'
import { clearToken, getToken, saveToken } from './token'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => getToken())
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  const { data: me } = useQuery({
    queryKey: ME_QUERY_KEY,
    queryFn: getMyInfo,
    enabled: token !== null,
    retry: false,
  })

  const resetSession = useCallback(() => {
    clearToken()
    setToken(null)
    queryClient.clear()
  }, [queryClient])

  // 토큰이 만료되거나 로그아웃된 토큰이면 로그인 화면으로 보낸다
  useEffect(() => {
    setUnauthorizedHandler(() => {
      resetSession()
      navigate('/login', { replace: true, state: { message: '로그인이 만료됐어요. 다시 로그인해 주세요.' } })
    })
    return () => setUnauthorizedHandler(null)
  }, [navigate, resetSession])

  const loginWithToken = useCallback(
    (newToken: string) => {
      saveToken(newToken)
      queryClient.clear()
      setToken(newToken)
    },
    [queryClient],
  )

  const logout = useCallback(async () => {
    try {
      await logoutApi()
    } finally {
      resetSession()
      navigate('/login', { replace: true, state: { message: '로그아웃했어요.' } })
    }
  }, [navigate, resetSession])

  const value = useMemo<AuthContextValue>(
    () => ({ isLoggedIn: token !== null, me, loginWithToken, logout }),
    [token, me, loginWithToken, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
