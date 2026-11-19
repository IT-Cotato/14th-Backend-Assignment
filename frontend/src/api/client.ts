import axios from 'axios'

const TOKEN_KEY = 'accessToken'

// localStorage는 시크릿 창이나 차단 설정에서 예외를 던질 수 있어서 감싸요
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
      // 저장하지 못해도 이번 화면에서는 메모리의 토큰으로 동작해요
    }
  },
  clear() {
    try {
      localStorage.removeItem(TOKEN_KEY)
    } catch {
      // 무시해요
    }
  },
}

// API 주소는 VITE_API_BASE_URL 환경변수로 넣어요 (frontend/.env.example 참고)
export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
})

// 저장한 토큰이 있으면 모든 요청에 붙여요.
// 공개 API도 토큰을 보내야 서버가 isAuthor를 계산할 수 있어요.
apiClient.interceptors.request.use((config) => {
  const token = tokenStorage.get()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 토큰을 보냈는데 401이 오면(만료, 무효, 로그아웃한 토큰) 토큰을 지우고 로그인 화면으로 보내요.
// 토큰 없이 받은 401(예: 로그인 실패)은 화면에서 에러 메시지로 보여줘야 해서 그대로 넘겨요.
apiClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (
      axios.isAxiosError(error) &&
      error.response?.status === 401 &&
      error.config?.headers?.Authorization
    ) {
      tokenStorage.clear()
      if (window.location.pathname !== '/login') {
        window.location.assign('/login')
      }
    }
    return Promise.reject(error)
  },
)
