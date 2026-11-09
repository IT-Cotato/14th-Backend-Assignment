import { useState } from 'react'
import { useAuth } from './AuthContext'

// 헤더와 마이페이지가 같은 로그아웃 흐름을 쓰도록 묶었어요.
// 서버가 무효화를 확인했을 때(또는 토큰이 이미 무효일 때)는 AuthProvider가 로그인 화면으로 보내요.
// 확인하지 못하면 onUnconfirmed를 불러서 안내를 보여줘요.
export function useLogout(onUnconfirmed: () => void) {
    const { logout, logoutLocally } = useAuth()
    const [loggingOut, setLoggingOut] = useState(false)

    const run = async () => {
        if (loggingOut) return
        setLoggingOut(true)
        const result = await logout()
        setLoggingOut(false)
        if (result === 'unconfirmed') onUnconfirmed()
    }

    // 사용자가 직접 선택한 경우에만 서버 확인 없이 이 기기의 토큰을 지워요.
    return { loggingOut, run, runLocally: logoutLocally }
}
