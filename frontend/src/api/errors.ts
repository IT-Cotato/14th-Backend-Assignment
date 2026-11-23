import { isAxiosError } from 'axios'
import type { ErrorResponse } from '../types/api'

export interface ApiError {
    status?: number
    code?: string
    message: string
}

const FALLBACK_MESSAGES: Record<number, string> = {
    400: '입력값을 다시 확인해 주세요.',
    401: '로그인이 필요해요.',
    403: '권한이 없어요.',
    404: '요청한 정보를 찾을 수 없어요.',
    409: '요청이 현재 상태와 충돌해요.',
    500: '서버에 문제가 생겼어요. 잠시 후 다시 시도해 주세요.',
}

function isErrorResponse(value: unknown): value is ErrorResponse {
    return (
        typeof value === 'object' &&
        value !== null &&
        'code' in value &&
        'message' in value
    )
}

// Axios 오류를 화면에 보여줄 수 있는 형태로 바꿔요.
export function toApiError(error: unknown): ApiError {
    if (isAxiosError(error)) {
        if (!error.response) {
            return { message: '서버에 연결할 수 없어요. 잠시 후 다시 시도해 주세요.' }
        }
        const { status, data } = error.response
        if (isErrorResponse(data)) {
            return { status, code: data.code, message: data.message }
        }
        return {
            status,
            message: FALLBACK_MESSAGES[status] ?? '알 수 없는 오류가 발생했어요.',
        }
    }
    return { message: '알 수 없는 오류가 발생했어요.' }
}
