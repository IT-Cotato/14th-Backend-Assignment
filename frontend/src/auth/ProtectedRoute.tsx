import { Navigate, Outlet } from 'react-router'
import { useAuth } from './AuthContext'

/** 로그인이 필요한 화면을 감싼다. 로그인하지 않았으면 로그인 화면으로 보낸다. */
export function ProtectedRoute() {
  const { isLoggedIn } = useAuth()

  if (!isLoggedIn) {
    return <Navigate to="/login" replace />
  }
  return <Outlet />
}
