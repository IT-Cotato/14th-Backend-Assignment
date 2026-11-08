import { startTransition, useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useNavigate } from 'react-router'
import * as authApi from '../api/auth'
import { toApiError } from '../api/errors'
import { clearToken, getToken, onAuthExpired, setToken } from '../api/tokenStorage'
import type { LoginRequest, User } from '../types/auth'
import { AuthContext, LOGOUT_MESSAGES, type AuthContextValue, type LogoutResult } from './AuthContext'

export default function AuthProvider({ children }: { children: ReactNode }) {
    const navigate = useNavigate()
    const [user, setUser] = useState<User | null>(null)
    const [initializing, setInitializing] = useState<boolean>(() => getToken() !== null)

    // 새로고침해도 로그인이 유지되도록 저장된 토큰으로 내 정보를 불러와요.
    useEffect(() => {
        if (!getToken()) return
        let ignore = false
        authApi
            .getMe()
            .then((me) => {
                if (!ignore) setUser(me)
            })
            .catch(() => {
                if (!ignore) {
                    clearToken()
                    setUser(null)
                }
            })
            .finally(() => {
                if (!ignore) setInitializing(false)
            })
        return () => {
            ignore = true
        }
    }, [])

    // 401이 오면 로그아웃 상태로 바꾸고, 필요하면 로그인 화면으로 보내요.
    useEffect(() => {
        return onAuthExpired((redirect) => {
            setUser(null)
            if (redirect && window.location.pathname !== '/login') {
                navigate('/login', {
                    state: {
                        from: window.location.pathname + window.location.search,
                        message: '로그인이 필요해요. 다시 로그인해 주세요.',
                    },
                })
            }
        })
    }, [navigate])

    const login = useCallback(async (request: LoginRequest) => {
        const { accessToken } = await authApi.login(request)
        setToken(accessToken)
        try {
            setUser(await authApi.getMe())
        } catch (error) {
            clearToken()
            throw error
        }
    }, [])

    // 로그인 상태를 지우고 안내 문구와 함께 로그인 화면으로 보내요.
    // React Router는 화면 이동을 startTransition으로 처리해요. user만 먼저 null로 바뀌면
    // 이전 주소(예: /mypage)의 RequireAuth가 "로그인이 필요한 페이지예요."로 이동을 덮어써요.
    // 그래서 두 변경을 같은 transition에 넣어 한 번에 반영해요.
    const endSession = useCallback(
        (message: string) => {
            clearToken()
            startTransition(() => {
                setUser(null)
                navigate('/login', { replace: true, state: { message } })
            })
        },
        [navigate],
    )

    // 서버가 토큰 무효화를 확인했을 때만 로그아웃 성공으로 처리해요.
    // 네트워크·서버 오류면 토큰을 남겨서 다시 시도할 수 있게 해요.
    // (토큰을 지워 버리면 다시 무효화 요청을 보낼 수 없어서, 만료 시각까지 토큰이 살아 있어요.)
    const logout = useCallback(async (): Promise<LogoutResult> => {
        try {
            await authApi.logout()
        } catch (error) {
            if (toApiError(error).status === 401) {
                // 이미 무효화됐거나 만료된 토큰이라 서버에서 쓸 수 없어요.
                endSession(LOGOUT_MESSAGES.alreadyInvalid)
                return 'alreadyInvalid'
            }
            return 'unconfirmed'
        }
        endSession(LOGOUT_MESSAGES.revoked)
        return 'revoked'
    }, [endSession])

    const logoutLocally = useCallback(() => {
        endSession(LOGOUT_MESSAGES.local)
    }, [endSession])

    const value = useMemo<AuthContextValue>(
        () => ({ user, initializing, isLoggedIn: user !== null, login, logout, logoutLocally }),
        [user, initializing, login, logout, logoutLocally],
    )

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
