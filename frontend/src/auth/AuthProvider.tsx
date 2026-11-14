import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useNavigate } from 'react-router'
import * as authApi from '../api/auth'
import { setUnauthorizedHandler } from '../api/client'
import { getErrorMessage } from '../api/errors'
import type { MyInfoResponse } from '../api/types'
import { AuthContext, type AuthContextValue } from './AuthContext'
import { tokenStorage } from './tokenStorage'

export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate()
  const [token, setToken] = useState<string | null>(() => tokenStorage.get())
  const [me, setMe] = useState<MyInfoResponse | null>(null)
  const [meError, setMeError] = useState<string | null>(null)
  // 토큰은 있는데 내 정보도 에러도 아직 없으면 불러오는 중입니다. (상태로 따로 두지 않고 계산합니다)
  const isLoadingMe = token !== null && me === null && meError === null

  const clearSession = useCallback(() => {
    tokenStorage.clear()
    setToken(null)
    setMe(null)
    setMeError(null)
  }, [])

  // 로그인/회원가입이 아닌 요청에서 401이 오면 세션을 비우고 로그인 화면으로 보냅니다.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      clearSession()
      navigate('/login', { replace: true })
    })
    return () => setUnauthorizedHandler(null)
  }, [clearSession, navigate])

  // 토큰이 생기면 내 정보를 불러옵니다. (헤더 닉네임, 마이페이지)
  useEffect(() => {
    if (!token) {
      return
    }
    // 로그아웃할 때 clearSession이 me와 meError를 비우므로, 새 토큰은 항상 빈 상태에서 시작합니다.
    let cancelled = false
    authApi
      .getMe()
      .then((info) => {
        if (!cancelled) setMe(info)
      })
      .catch((error: unknown) => {
        if (!cancelled) setMeError(getErrorMessage(error))
      })
    return () => {
      cancelled = true
    }
  }, [token])

  const login = useCallback(async (email: string, password: string) => {
    const { accessToken } = await authApi.login({ email, password })
    tokenStorage.set(accessToken)
    setToken(accessToken)
  }, [])

  // 서버 로그아웃이 실패해도(이미 무효인 토큰 등) 토큰은 지우고 로그인 화면으로 갑니다.
  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } catch {
      // 401은 인터셉터가 이미 처리했고, 그 밖의 실패도 로그아웃을 막지 않습니다.
    } finally {
      clearSession()
      navigate('/login', { replace: true })
    }
  }, [clearSession, navigate])

  const value = useMemo<AuthContextValue>(
    () => ({ isLoggedIn: token !== null, me, isLoadingMe, meError, login, logout }),
    [token, me, isLoadingMe, meError, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
