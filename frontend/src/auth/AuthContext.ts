import { createContext, useContext } from 'react'
import type { MeResponse } from '../api/auth'

export interface AuthContextValue {
  user: MeResponse | null
  initializing: boolean
  login: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext)
  if (!value) {
    throw new Error('useAuth는 AuthProvider 안에서만 쓸 수 있습니다.')
  }
  return value
}
