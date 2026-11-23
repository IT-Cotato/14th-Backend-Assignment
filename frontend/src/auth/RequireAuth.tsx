import { Navigate, Outlet } from 'react-router-dom'
import { Loading } from '../components/Loading'
import { useAuth } from './useAuth'

// 로그인이 필요한 화면. 로그인하지 않았으면 /login으로 보내요.
export function RequireAuth() {
  const { status } = useAuth()
  if (status === 'loading') return <Loading />
  if (status === 'anonymous') return <Navigate to="/login" replace />
  return <Outlet />
}
