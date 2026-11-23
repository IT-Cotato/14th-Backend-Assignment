// 로그인 토큰을 브라우저(localStorage)에 보관한다
const TOKEN_KEY = 'accessToken'

export function getToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY)
  } catch {
    return null
  }
}

export function setToken(token: string) {
  try {
    localStorage.setItem(TOKEN_KEY, token)
  } catch {
    // 저장소를 쓸 수 없는 환경이면 로그인 유지가 안 될 뿐이므로 무시한다
  }
}

export function clearToken() {
  try {
    localStorage.removeItem(TOKEN_KEY)
  } catch {
    // 위와 같다
  }
}
