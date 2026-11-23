import { startTransition, useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
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

  /**
   * 로그인 화면 이동과 로그인 상태 해제를 한 번에 반영한다.
   * React Router 의 이동은 transition 으로 처리되므로 상태 해제도 같은 transition 에 넣는다.
   * 따로 반영되면 로그인이 필요한 화면의 RequireAuth 가 먼저 리다이렉트해서 안내 문구가 바뀐다.
   */
  const endSession = useCallback(
    (message: string) => {
      clearToken()
      startTransition(() => {
        navigate('/login', { replace: true, state: { message } })
        setToken(null)
      })
      queryClient.removeQueries({ queryKey: ME_QUERY_KEY })
    },
    [navigate, queryClient],
  )

  // 토큰이 만료되거나 로그아웃된 토큰이면 로그인 화면으로 보낸다
  useEffect(() => {
    setUnauthorizedHandler(() => endSession('로그인이 만료됐어요. 다시 로그인해 주세요.'))
    return () => setUnauthorizedHandler(null)
  }, [endSession])

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
      endSession('로그아웃했어요.')
    }
  }, [endSession])

  const value = useMemo<AuthContextValue>(
    () => ({ isLoggedIn: token !== null, me, loginWithToken, logout }),
    [token, me, loginWithToken, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
