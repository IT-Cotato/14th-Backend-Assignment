import axios from 'axios'
import type { ErrorResponse } from './types'

/** 서버가 내려준 에러 메시지를 그대로 꺼낸다. */
export function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ErrorResponse>(error)) {
    if (error.response?.data?.message) {
      return error.response.data.message
    }
    if (!error.response) {
      return '서버에 연결할 수 없어요. 잠시 후 다시 시도해 주세요.'
    }
  }
  return '알 수 없는 오류가 발생했어요.'
}

export function getErrorStatus(error: unknown): number | undefined {
  return axios.isAxiosError(error) ? error.response?.status : undefined
}
