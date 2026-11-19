import { Navigate, Outlet, useLocation } from 'react-router-dom'

import { Loading } from '../components/StatusMessage'
import { useAuth } from './authContext'

// 로그인이 필요한 화면을 감싸요. 로그인하지 않았으면 로그인 화면으로 보내요.
// 로그인 후 원래 가려던 화면으로 돌아올 수 있게 주소를 state에 담아요.
export function RequireAuth() {
  const { isLoggedIn, isLoading } = useAuth()
  const location = useLocation()

  if (!isLoggedIn) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  if (isLoading) {
    return <Loading />
  }
  return <Outlet />
}
