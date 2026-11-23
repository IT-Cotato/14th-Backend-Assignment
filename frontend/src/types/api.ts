// 백엔드 공통 응답 형식이에요. (docs/api-spec.md 2장)

export interface ApiResponse<T> {
    success: true
    status: number
    data: T
    timestamp: string
}

export interface ErrorResponse {
    success: false
    status: number
    code: string
    message: string
    method: string
    path: string
    timestamp: string
}

// 목록 응답의 페이지 형식이에요. page는 0부터 시작해요.
export interface PageResponse<T> {
    content: T[]
    page: number
    size: number
    totalElements: number
    totalPages: number
}

export const PARTS = ['PM', 'DE', 'BE', 'FE'] as const
export type Part = (typeof PARTS)[number]

export const PART_LABELS: Record<Part, string> = {
    PM: '기획(PM)',
    DE: '디자인(DE)',
    BE: '백엔드(BE)',
    FE: '프론트엔드(FE)',
}

export interface Author {
    nickname: string
    generation: number
    part: Part
}
