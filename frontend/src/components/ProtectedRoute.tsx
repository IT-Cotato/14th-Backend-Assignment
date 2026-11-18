import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/useAuth.ts'

// 로그인한 사용자만 들어갈 수 있는 화면을 감싼다. 로그인하지 않았으면 로그인 화면으로 보낸다.
export default function ProtectedRoute() {
  const { user, loading } = useAuth()

  if (loading) {
    return <p className="text-gray-500">불러오는 중...</p>
  }
  if (!user) {
    return <Navigate to="/login" replace />
  }
  return <Outlet />
}
