import { createContext, useContext } from 'react'
import type { LoginRequest, User } from '../types'

export interface AuthContextValue {
  /** 토큰을 가지고 있는지 여부 */
  isLoggedIn: boolean
  /** 내 정보. 로그인하지 않았거나 아직 불러오는 중이면 null */
  user: User | null
  login: (request: LoginRequest) => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext)
  if (!value) {
    throw new Error('useAuth 는 AuthProvider 안에서만 쓸 수 있습니다.')
  }
  return value
}
