import { createContext } from 'react'
import type { User } from '../api/types.ts'

export interface AuthContextValue {
  user: User | null
  // 저장된 토큰으로 내 정보를 불러오는 중인지
  loading: boolean
  login: (token: string) => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)
