import axios from 'axios'
import type { ErrorResponse } from './types'

const NETWORK_ERROR_MESSAGE = '서버에 연결할 수 없어요. 잠시 후 다시 시도해 주세요.'
const UNKNOWN_ERROR_MESSAGE = '알 수 없는 오류가 발생했어요. 잠시 후 다시 시도해 주세요.'

// 서버가 내려준 ErrorResponse.message를 꺼내요. 응답이 없으면(서버 꺼짐 등) 연결 오류 문구를 써요.
export function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ErrorResponse>(error)) {
    if (!error.response) {
      return NETWORK_ERROR_MESSAGE
    }
    return error.response.data?.message ?? UNKNOWN_ERROR_MESSAGE
  }
  return UNKNOWN_ERROR_MESSAGE
}
