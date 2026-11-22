import { expect, type APIRequestContext, type APIResponse } from '@playwright/test'
import type { TestUser } from './data.ts'

export type StudyStatus = 'RECRUITING' | 'CLOSED'

export interface PostBody {
  title?: string
  content?: string
  studyStatus?: StudyStatus
}

function authHeaders(token?: string): Record<string, string> {
  return token ? { Authorization: `Bearer ${token}` } : {}
}

// 백엔드를 직접 부르는 도우미. 준비 단계(가입·글 작성)와, 화면에서 숨긴 동작을 서버가 막는지 확인할 때 써요.
// 상태 코드를 확인해야 하는 호출은 APIResponse를 그대로 돌려줘요.
export class ApiClient {
  private readonly request: APIRequestContext

  constructor(request: APIRequestContext) {
    this.request = request
  }

  signup(user: TestUser): Promise<APIResponse> {
    return this.request.post('/api/auth/signup', {
      data: {
        email: user.email,
        password: user.password,
        name: user.name,
        nickname: user.nickname,
        generation: user.generation,
        part: user.part,
      },
    })
  }

  async login(email: string, password: string): Promise<string> {
    const res = await this.request.post('/api/auth/login', { data: { email, password } })
    expect(res.status(), '테스트 준비: 로그인').toBe(200)
    return (await res.json()).data.accessToken
  }

  getMe(token?: string): Promise<APIResponse> {
    return this.request.get('/api/users/me', { headers: authHeaders(token) })
  }

  createPostRaw(token: string | undefined, body: PostBody): Promise<APIResponse> {
    return this.request.post('/api/study-posts', { data: body, headers: authHeaders(token) })
  }

  async createPost(token: string, body: { title: string; content: string }): Promise<number> {
    const res = await this.createPostRaw(token, body)
    expect(res.status(), '테스트 준비: 모집글 작성').toBe(201)
    return (await res.json()).data.postId
  }

  getPost(postId: number, token?: string): Promise<APIResponse> {
    return this.request.get(`/api/study-posts/${postId}`, { headers: authHeaders(token) })
  }

  updatePost(postId: number, token: string, body: PostBody): Promise<APIResponse> {
    return this.request.patch(`/api/study-posts/${postId}`, { data: body, headers: authHeaders(token) })
  }

  deletePost(postId: number, token: string): Promise<APIResponse> {
    return this.request.delete(`/api/study-posts/${postId}`, { headers: authHeaders(token) })
  }

  createComment(postId: number, token: string, content: string): Promise<APIResponse> {
    return this.request.post(`/api/study-posts/${postId}/comments`, {
      data: { content },
      headers: authHeaders(token),
    })
  }

  async getComments(postId: number): Promise<{ content: { commentId: number; content: string }[]; totalElements: number }> {
    const res = await this.request.get(`/api/study-posts/${postId}/comments`)
    expect(res.status(), '댓글 목록 조회').toBe(200)
    return (await res.json()).data
  }

  updateComment(commentId: number, token: string, content: string): Promise<APIResponse> {
    return this.request.patch(`/api/comments/${commentId}`, { data: { content }, headers: authHeaders(token) })
  }

  deleteComment(commentId: number, token: string): Promise<APIResponse> {
    return this.request.delete(`/api/comments/${commentId}`, { headers: authHeaders(token) })
  }
}

// 서버가 막았는지 상태 코드와 에러 code(예: STUDY-002)를 함께 확인해요
export async function expectError(res: APIResponse, status: number, code: string): Promise<void> {
  expect(res.status()).toBe(status)
  expect((await res.json()).code).toBe(code)
}
