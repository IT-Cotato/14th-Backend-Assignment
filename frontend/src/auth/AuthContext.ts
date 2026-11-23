import { createContext, useContext } from 'react'
import type { LoginRequest, User } from '../types/auth'

// 로그아웃 결과예요.
// - revoked: 서버가 토큰 무효화를 확인했어요. (204)
// - alreadyInvalid: 서버가 401을 돌려줬어요. 토큰이 이미 만료됐거나 무효화된 상태예요.
// - unconfirmed: 네트워크 오류나 서버 오류로 무효화를 확인하지 못했어요. 로그인 상태를 유지해요.
export type LogoutResult = 'revoked' | 'alreadyInvalid' | 'unconfirmed'

export interface AuthContextValue {
    user: User | null
    // 저장된 토큰으로 내 정보를 확인하는 중이면 true예요.
    initializing: boolean
    isLoggedIn: boolean
    login: (request: LoginRequest) => Promise<void>
    // revoked, alreadyInvalid면 토큰을 지우고 로그인 화면으로 이동해요.
    logout: () => Promise<LogoutResult>
    // 서버 확인 없이 이 브라우저의 토큰만 지우고 로그인 화면으로 이동해요. 사용자가 직접 선택했을 때만 써요.
    logoutLocally: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth(): AuthContextValue {
    const value = useContext(AuthContext)
    if (!value) {
        throw new Error('useAuth는 AuthProvider 안에서만 사용할 수 있어요.')
    }
    return value
}

// 로그아웃 후 로그인 화면에 보여줄 안내 문구예요.
export const LOGOUT_MESSAGES = {
    revoked: '로그아웃했어요.',
    alreadyInvalid: '로그인 정보가 이미 만료되어 로그아웃 상태로 전환했어요.',
    local: '이 기기에서 로그아웃했어요. 서버에서 토큰 무효화는 확인하지 못해서, 기존 토큰은 만료 시각까지 유효할 수 있어요.',
} as const

// 서버 로그아웃을 확인하지 못했을 때 보여줄 안내 문구예요.
export const LOGOUT_UNCONFIRMED_MESSAGE =
    '서버에서 로그아웃(토큰 무효화)을 확인하지 못했어요. 아직 로그인 상태를 유지하고 있어요. 다시 시도하거나, 이 기기에서만 로그아웃할 수 있어요.'
