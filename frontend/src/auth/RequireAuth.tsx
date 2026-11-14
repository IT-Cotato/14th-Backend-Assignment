import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import { useAuth } from './AuthContext'

// 로그인이 필요한 화면에 로그인 없이 들어오면 로그인 화면으로 보냅니다.
export function RequireAuth({ children }: { children: ReactNode }) {
  const { isLoggedIn } = useAuth()
  const location = useLocation()

  if (!isLoggedIn) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  return children
}
