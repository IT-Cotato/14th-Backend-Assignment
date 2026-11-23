import { Navigate, Outlet } from 'react-router-dom'
import { Loading } from '../components/Loading'
import { useAuth } from './useAuth'

// 로그인·회원가입 화면. 이미 로그인했으면 /studies로 보내요.
export function GuestOnly() {
  const { status } = useAuth()
  if (status === 'loading') return <Loading />
  if (status === 'authenticated') return <Navigate to="/studies" replace />
  return <Outlet />
}
