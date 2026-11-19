import { createContext, useContext } from 'react'

import type { User } from '../api/types'

export interface AuthContextValue {
  // 내 정보. 로그인하지 않았거나 아직 불러오는 중이면 null
  user: User | null
  // 저장된 토큰이 있으면 true (내 정보를 불러오기 전에도 true)
  isLoggedIn: boolean
  // 토큰은 있는데 내 정보를 아직 불러오는 중이면 true
  isLoading: boolean
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
