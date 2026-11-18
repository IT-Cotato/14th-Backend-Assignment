import { randomBytes, randomUUID } from 'node:crypto'
import { expect, type APIRequestContext, type APIResponse } from '@playwright/test'

// 테스트의 "준비 단계"(가입, 글 작성 등)는 화면 대신 백엔드 API를 직접 호출해서 만든다.
// 그래야 검증하려는 화면 조작만 브라우저로 확인하고, 테스트도 빨라진다.
export const API_URL = process.env.E2E_API_URL ?? 'http://localhost:8080'

export type Part = 'PM' | 'DE' | 'BE' | 'FE'

export interface TestUser {
  email: string
  password: string
  name: string
  nickname: string
  generation: number
  part: Part
}

// 테스트마다 겹치지 않는 값을 만들기 위한 짧은 무작위 문자열
export function uniqueToken(): string {
  return randomBytes(4).toString('hex')
}

// 이메일·닉네임·비밀번호는 매번 새로 만든다. 비밀번호는 코드에 적지 않고 실행할 때 생성한다.
export function newUser(overrides: Partial<TestUser> = {}): TestUser {
  return {
    email: `e2e-${randomUUID()}@example.com`,
    password: randomBytes(12).toString('base64url'),
    name: 'E2E 테스터',
    nickname: `e2e${randomBytes(3).toString('hex')}`,
    generation: 14,
    part: 'BE',
    ...overrides,
  }
}

type HttpMethod = 'GET' | 'POST' | 'PATCH' | 'DELETE'

interface CallOptions {
  token?: string
  data?: unknown
}

// 상태 코드를 직접 검사해야 하는 호출(403, 409 등)에 쓰는 저수준 함수
export async function callApi(
  request: APIRequestContext,
  method: HttpMethod,
  path: string,
  options: CallOptions = {},
): Promise<APIResponse> {
  const headers: Record<string, string> = {}
  if (options.token) {
    headers.Authorization = `Bearer ${options.token}`
  }
  return request.fetch(`${API_URL}${path}`, { method, headers, data: options.data })
}

// 성공해야 하는 준비 단계 호출. 실패하면 응답 본문과 함께 바로 테스트를 실패시킨다.
async function callOk<T>(
  request: APIRequestContext,
  method: HttpMethod,
  path: string,
  options: CallOptions = {},
): Promise<T> {
  const response = await callApi(request, method, path, options)
  expect(response.ok(), `${method} ${path} → ${response.status()} ${await response.text()}`).toBeTruthy()
  const body = (await response.json()) as { data: T }
  return body.data
}

export async function signupByApi(request: APIRequestContext, user: TestUser): Promise<void> {
  await callOk(request, 'POST', '/api/auth/signup', { data: user })
}

export async function loginByApi(
  request: APIRequestContext,
  user: Pick<TestUser, 'email' | 'password'>,
): Promise<string> {
  const data = await callOk<{ accessToken: string }>(request, 'POST', '/api/auth/login', {
    data: { email: user.email, password: user.password },
  })
  return data.accessToken
}

// 새 사용자를 가입시키고 토큰까지 받아 둔다
export async function createUser(
  request: APIRequestContext,
  overrides: Partial<TestUser> = {},
): Promise<{ user: TestUser; token: string }> {
  const user = newUser(overrides)
  await signupByApi(request, user)
  const token = await loginByApi(request, user)
  return { user, token }
}

export async function createPostByApi(
  request: APIRequestContext,
  token: string,
  post: { title: string; content?: string },
): Promise<number> {
  const data = await callOk<{ postId: number }>(request, 'POST', '/api/study-posts', {
    token,
    data: { title: post.title, content: post.content ?? '테스트용 모집글 내용이에요.' },
  })
  return data.postId
}

export async function closePostByApi(request: APIRequestContext, token: string, postId: number): Promise<void> {
  await callOk(request, 'PATCH', `/api/study-posts/${postId}`, { token, data: { studyStatus: 'CLOSED' } })
}

export async function createCommentByApi(
  request: APIRequestContext,
  token: string,
  postId: number,
  content: string,
): Promise<number> {
  const data = await callOk<{ commentId: number }>(request, 'POST', `/api/study-posts/${postId}/comments`, {
    token,
    data: { content },
  })
  return data.commentId
}

// 에러 응답 본문의 code 값 (예: STUDY-002)
export async function errorCodeOf(response: APIResponse): Promise<string> {
  const body = (await response.json()) as { code?: string }
  return body.code ?? ''
}
