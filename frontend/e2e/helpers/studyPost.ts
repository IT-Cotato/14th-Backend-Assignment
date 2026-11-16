import type { APIRequestContext } from '@playwright/test'
import type { CommentIdResponse } from '../../src/api/comments'
import type { StudyPostIdResponse } from '../../src/api/studyPosts'
import { API_URL, authHeaders, readData } from './auth'
import { uniqueSuffix } from './user'

export interface TestPost {
  postId: number
  title: string
  content: string
}

// 테스트마다 새 제목/내용을 만든다. 제목은 50자 이하
export function createPostData(): { title: string; content: string } {
  const suffix = uniqueSuffix(8)
  return {
    title: `E2E 스터디 ${suffix}`,
    content: `E2E 테스트용 모집글입니다. ${suffix}`,
  }
}

export async function createPostViaApi(
  request: APIRequestContext,
  token: string,
  overrides: Partial<Omit<TestPost, 'postId'>> = {},
): Promise<TestPost> {
  const data = { ...createPostData(), ...overrides }
  const response = await request.post(`${API_URL}/api/study-posts`, {
    headers: authHeaders(token),
    data,
  })
  const { postId } = await readData<StudyPostIdResponse>(response, 201, '모집글 작성')
  return { postId, ...data }
}

export async function closePostViaApi(
  request: APIRequestContext,
  token: string,
  postId: number,
): Promise<void> {
  const response = await request.patch(`${API_URL}/api/study-posts/${postId}`, {
    headers: authHeaders(token),
    data: { studyStatus: 'CLOSED' },
  })
  await readData<StudyPostIdResponse>(response, 200, '모집 마감')
}

export async function createCommentViaApi(
  request: APIRequestContext,
  token: string,
  postId: number,
  content = `E2E 신청 댓글 ${uniqueSuffix(8)}`,
): Promise<{ commentId: number; content: string }> {
  const response = await request.post(`${API_URL}/api/study-posts/${postId}/comments`, {
    headers: authHeaders(token),
    data: { content },
  })
  const { commentId } = await readData<CommentIdResponse>(response, 201, '댓글 작성')
  return { commentId, content }
}
