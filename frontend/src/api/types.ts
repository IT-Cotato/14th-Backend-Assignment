// 백엔드 응답 타입입니다. 필드명은 docs/api-spec.md와 백엔드 공통 응답을 그대로 따릅니다.

// 성공 응답 래퍼 (백엔드 ApiResponse<T>)
export interface ApiResponse<T> {
  success: true
  status: number
  data: T
  timestamp: string
}

// 실패 응답 (백엔드 ErrorResponse)
export interface ErrorResponse {
  success: false
  status: number
  code: string
  message: string
  method: string
  path: string
  timestamp: string
}

export const PARTS = ['PM', 'DE', 'BE', 'FE'] as const
export type Part = (typeof PARTS)[number]

export const PART_LABELS: Record<Part, string> = {
  PM: '기획',
  DE: '디자인',
  BE: '백엔드',
  FE: '프론트엔드',
}

// 1. 회원가입
export interface SignupRequest {
  email: string
  password: string
  name: string
  nickname: string
  generation: number
  part: Part
}

export interface SignupResponse {
  userId: number
}

// 2. 로그인
export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
}

// 4. 내 정보 조회
export interface MyInfoResponse {
  userId: number
  email: string
  name: string
  nickname: string
  generation: number
  part: Part
}
