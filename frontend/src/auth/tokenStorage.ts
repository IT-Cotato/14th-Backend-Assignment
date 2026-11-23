const KEY = 'accessToken'

// localStorage가 막힌 환경(시크릿 모드, 저장소 차단 등)에서도 앱이 죽지 않게 감싼다.
export const tokenStorage = {
  get(): string | null {
    try {
      return localStorage.getItem(KEY)
    } catch {
      return null
    }
  },
  set(token: string): void {
    try {
      localStorage.setItem(KEY, token)
    } catch {
      // 저장하지 못하면 새로고침 시 로그인이 풀리지만 현재 화면 동작에는 영향이 없다.
    }
  },
  clear(): void {
    try {
      localStorage.removeItem(KEY)
    } catch {
      // 지우지 못해도 서버가 로그아웃된 토큰을 거부한다.
    }
  },
}
