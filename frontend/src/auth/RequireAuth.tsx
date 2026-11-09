import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import { useAuth } from './AuthContext'
import LoadingState from '../components/LoadingState'

// 로그인하지 않은 사용자는 로그인 화면으로 보내고, 로그인 후 원래 페이지로 돌아오게 해요.
export default function RequireAuth({ children }: { children: ReactNode }) {
    const { isLoggedIn, initializing } = useAuth()
    const location = useLocation()

    if (initializing) {
        return <LoadingState message="로그인 정보를 확인하고 있어요..." />
    }

    if (!isLoggedIn) {
        return (
            <Navigate
                to="/login"
                replace
                state={{
                    from: location.pathname + location.search,
                    message: '로그인이 필요한 페이지예요.',
                }}
            />
        )
    }

    return <>{children}</>
}
