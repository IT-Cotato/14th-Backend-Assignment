import { createContext, useContext } from 'react'
import type { MyInfoResponse } from '../api/types'

export interface AuthContextValue {
  // 로그인 여부는 토큰이 있는지로 판단합니다.
  isLoggedIn: boolean
  // 내 정보 (GET /api/users/me). 불러오기 전이면 null입니다.
  me: MyInfoResponse | null
  isLoadingMe: boolean
  meError: string | null
  login: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext)
  if (!value) {
    throw new Error('useAuth는 AuthProvider 안에서만 쓸 수 있어요.')
  }
  return value
}
