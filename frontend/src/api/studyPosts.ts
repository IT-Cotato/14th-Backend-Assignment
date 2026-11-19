import { client } from './client'
import type { ApiResponse, PageResponse, Part, StudyPostDetail, StudyPostSummary, StudyStatus } from './types'

export interface StudyPostQuery {
  keyword?: string
  status?: StudyStatus | ''
  part?: Part | ''
  page?: number
  size?: number
}

export async function getStudyPosts(query: StudyPostQuery) {
  const params = Object.fromEntries(
    Object.entries(query).filter(([, v]) => v !== '' && v !== undefined),
  )
  const res = await client.get<ApiResponse<PageResponse<StudyPostSummary>>>('/api/study-posts', { params })
  return res.data.data
}

export async function getStudyPost(postId: number) {
  const res = await client.get<ApiResponse<StudyPostDetail>>(`/api/study-posts/${postId}`)
  return res.data.data
}

export async function createStudyPost(body: { title: string; content: string }) {
  const res = await client.post<ApiResponse<{ postId: number }>>('/api/study-posts', body)
  return res.data.data
}

export async function updateStudyPost(
  postId: number,
  body: { title?: string; content?: string; studyStatus?: StudyStatus },
) {
  const res = await client.patch<ApiResponse<{ postId: number }>>(`/api/study-posts/${postId}`, body)
  return res.data.data
}

export async function deleteStudyPost(postId: number) {
  await client.delete(`/api/study-posts/${postId}`)
}
