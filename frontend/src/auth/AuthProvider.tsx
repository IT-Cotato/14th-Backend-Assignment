import { useEffect, useState, type ReactNode } from 'react'
import { getMe, logout as logoutApi } from '../api/auth.ts'
import { clearToken, getToken, setToken } from '../api/token.ts'
import type { User } from '../api/types.ts'
import { AuthContext } from './AuthContext.ts'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  // 토큰이 저장돼 있으면 내 정보를 확인할 때까지 "불러오는 중"으로 둔다
  const [loading, setLoading] = useState(() => getToken() !== null)

  useEffect(() => {
    if (!getToken()) return
    getMe()
      .then(setUser)
      .catch(() => clearToken())
      .finally(() => setLoading(false))
  }, [])

  async function login(token: string) {
    setToken(token)
    try {
      setUser(await getMe())
    } catch (e) {
      clearToken()
      throw e
    }
  }

  async function logout() {
    try {
      await logoutApi()
    } catch {
      // 이미 만료된 토큰이어도 화면에서는 로그아웃 처리한다
    }
    clearToken()
    setUser(null)
  }

  return <AuthContext.Provider value={{ user, loading, login, logout }}>{children}</AuthContext.Provider>
}
