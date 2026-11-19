import { api } from './client'
import type {
  ApiResponse,
  PageResponse,
  StudyPostCreateRequest,
  StudyPostDetail,
  StudyPostSearchParams,
  StudyPostSummary,
  StudyPostUpdateRequest,
} from './types'

export const studyPostKeys = {
  all: ['studyPosts'] as const,
  list: (params: StudyPostSearchParams) => ['studyPosts', 'list', params] as const,
  detail: (postId: number) => ['studyPosts', 'detail', postId] as const,
}

export async function searchStudyPosts(params: StudyPostSearchParams) {
  const { data } = await api.get<ApiResponse<PageResponse<StudyPostSummary>>>('/api/study-posts', { params })
  return data.data
}

export async function getStudyPost(postId: number) {
  const { data } = await api.get<ApiResponse<StudyPostDetail>>(`/api/study-posts/${postId}`)
  return data.data
}

export async function createStudyPost(request: StudyPostCreateRequest) {
  const { data } = await api.post<ApiResponse<{ postId: number }>>('/api/study-posts', request)
  return data.data
}

export async function updateStudyPost(postId: number, request: StudyPostUpdateRequest) {
  const { data } = await api.patch<ApiResponse<{ postId: number }>>(`/api/study-posts/${postId}`, request)
  return data.data
}

export async function deleteStudyPost(postId: number) {
  await api.delete(`/api/study-posts/${postId}`)
}
