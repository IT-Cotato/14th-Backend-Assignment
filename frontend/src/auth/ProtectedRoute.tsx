import { Navigate, Outlet } from 'react-router'
import { useAuth } from './useAuth'

// 로그인이 필요한 화면을 감싼다. 로그인하지 않았으면 로그인 화면으로 보낸다.
export function ProtectedRoute() {
  const { isAuthenticated, loading } = useAuth()

  if (loading) {
    return <p className="py-10 text-center text-slate-500">불러오는 중이에요...</p>
  }
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }
  return <Outlet />
}
