import { createContext, useContext } from 'react'
import type { MyInfo } from '../api/types'

export const ME_QUERY_KEY = ['me']

export interface AuthContextValue {
  isLoggedIn: boolean
  me: MyInfo | undefined
  loginWithToken: (token: string) => void
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth() {
  const value = useContext(AuthContext)
  if (!value) {
    throw new Error('useAuth는 AuthProvider 안에서만 쓸 수 있어요.')
  }
  return value
}
