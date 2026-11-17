import { createContext } from 'react'
import type { UserMe } from '../api/types'

export interface AuthContextValue {
  user: UserMe | null
  isAuthenticated: boolean
  // 저장된 토큰으로 내 정보를 불러오는 중이면 true
  loading: boolean
  login: (accessToken: string) => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)
