import { client } from './client.ts'
import type {
  ApiResponse,
  PageResponse,
  StudyPostCreateRequest,
  StudyPostDetail,
  StudyPostListItem,
  StudyPostSearchParams,
  StudyPostUpdateRequest,
} from './types.ts'

export async function getStudyPosts(params: StudyPostSearchParams) {
  const res = await client.get<ApiResponse<PageResponse<StudyPostListItem>>>('/study-posts', { params })
  return res.data.data
}

export async function getStudyPost(postId: number) {
  const res = await client.get<ApiResponse<StudyPostDetail>>(`/study-posts/${postId}`)
  return res.data.data
}

export async function createStudyPost(body: StudyPostCreateRequest) {
  const res = await client.post<ApiResponse<{ postId: number }>>('/study-posts', body)
  return res.data.data
}

export async function updateStudyPost(postId: number, body: StudyPostUpdateRequest) {
  const res = await client.patch<ApiResponse<{ postId: number }>>(`/study-posts/${postId}`, body)
  return res.data.data
}

export async function deleteStudyPost(postId: number) {
  await client.delete(`/study-posts/${postId}`)
}
