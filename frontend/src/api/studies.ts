import { apiClient } from './client'
import type {
  ApiResponse,
  PageResponse,
  Part,
  StudyPostCreateRequest,
  StudyPostDetail,
  StudyPostIdResponse,
  StudyPostSummary,
  StudyPostUpdateRequest,
  StudyStatus,
} from './types'

export interface StudyListParams {
  keyword?: string
  status?: StudyStatus | ''
  part?: Part | ''
  page?: number
  size?: number
}

export async function fetchStudies(params: StudyListParams): Promise<PageResponse<StudyPostSummary>> {
  // 비어 있는 조건은 쿼리 문자열에서 뺀다. (서버는 값이 없으면 그 조건을 적용하지 않는다)
  const query = {
    keyword: params.keyword || undefined,
    status: params.status || undefined,
    part: params.part || undefined,
    page: params.page,
    size: params.size,
  }
  const { data } = await apiClient.get<ApiResponse<PageResponse<StudyPostSummary>>>('/api/study-posts', {
    params: query,
  })
  return data.data
}

export async function fetchStudy(postId: number): Promise<StudyPostDetail> {
  const { data } = await apiClient.get<ApiResponse<StudyPostDetail>>(`/api/study-posts/${postId}`)
  return data.data
}

export async function createStudy(body: StudyPostCreateRequest): Promise<StudyPostIdResponse> {
  const { data } = await apiClient.post<ApiResponse<StudyPostIdResponse>>('/api/study-posts', body)
  return data.data
}

export async function updateStudy(postId: number, body: StudyPostUpdateRequest): Promise<StudyPostIdResponse> {
  const { data } = await apiClient.patch<ApiResponse<StudyPostIdResponse>>(`/api/study-posts/${postId}`, body)
  return data.data
}

// 204 응답이라 본문이 없다.
export async function deleteStudy(postId: number): Promise<void> {
  await apiClient.delete(`/api/study-posts/${postId}`)
}
