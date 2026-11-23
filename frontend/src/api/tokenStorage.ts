// accessToken을 localStorage에 저장하고, 인증 만료 이벤트를 알려줘요.

const TOKEN_KEY = 'accessToken'

export function getToken(): string | null {
    try {
        return localStorage.getItem(TOKEN_KEY)
    } catch {
        return null
    }
}

export function setToken(token: string): void {
    try {
        localStorage.setItem(TOKEN_KEY, token)
    } catch {
        // 저장소를 쓸 수 없는 환경이면 무시해요.
    }
}

export function clearToken(): void {
    try {
        localStorage.removeItem(TOKEN_KEY)
    } catch {
        // 저장소를 쓸 수 없는 환경이면 무시해요.
    }
}

// redirect가 true면 로그인 화면으로 보내야 하는 401이에요.
export type AuthExpiredListener = (redirect: boolean) => void

const listeners = new Set<AuthExpiredListener>()

export function onAuthExpired(listener: AuthExpiredListener): () => void {
    listeners.add(listener)
    return () => {
        listeners.delete(listener)
    }
}

export function emitAuthExpired(redirect: boolean): void {
    listeners.forEach((listener) => listener(redirect))
}
