const TOKEN_KEY = 'accessToken'

// localStorage는 브라우저 설정에 따라 막힐 수 있어서, 실패해도 앱이 멈추지 않게 감싸요.
export const tokenStorage = {
  get(): string | null {
    try {
      return localStorage.getItem(TOKEN_KEY)
    } catch {
      return null
    }
  },
  set(token: string) {
    try {
      localStorage.setItem(TOKEN_KEY, token)
    } catch {
      // 저장하지 못하면 새로고침 후 로그인이 유지되지 않을 뿐이에요
    }
  },
  clear() {
    try {
      localStorage.removeItem(TOKEN_KEY)
    } catch {
      // 무시
    }
  },
}
