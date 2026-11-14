// accessToken을 localStorage에 저장합니다. 새로 고쳐도 로그인이 유지됩니다.
const TOKEN_KEY = 'accessToken'

export const tokenStorage = {
  get(): string | null {
    return localStorage.getItem(TOKEN_KEY)
  },
  set(token: string) {
    localStorage.setItem(TOKEN_KEY, token)
  },
  clear() {
    localStorage.removeItem(TOKEN_KEY)
  },
}
