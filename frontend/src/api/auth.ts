import api from './client'
import type { ApiResponse } from '../types/api'
import type {
    LoginRequest,
    LoginResponse,
    SignupRequest,
    SignupResponse,
    User,
} from '../types/auth'

// 백엔드 응답은 ApiResponse로 감싸져 있어서 data만 꺼내서 돌려줘요.

export async function signup(request: SignupRequest): Promise<SignupResponse> {
    const res = await api.post<ApiResponse<SignupResponse>>('/auth/signup', request, {
        skipAuth: true,
    })
    return res.data.data
}

export async function login(request: LoginRequest): Promise<LoginResponse> {
    const res = await api.post<ApiResponse<LoginResponse>>('/auth/login', request, {
        skipAuth: true,
    })
    return res.data.data
}

// 204 응답이라 본문이 없어요.
// 401(이미 무효화된 토큰)은 AuthProvider가 직접 처리해서 인터셉터의 로그인 이동을 꺼요.
export async function logout(): Promise<void> {
    await api.post('/auth/logout', undefined, { skipAuthExpiredHandling: true })
}

export async function getMe(): Promise<User> {
    const res = await api.get<ApiResponse<User>>('/users/me')
    return res.data.data
}
