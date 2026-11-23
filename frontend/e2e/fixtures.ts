import { randomUUID } from 'node:crypto'
import { expect, type APIRequestContext, type APIResponse, type Page } from '@playwright/test'

export function newAccount(part: 'BE' | 'FE' = 'BE') {
  const id = randomUUID().slice(0, 8)
  return { email: 'e2e-' + id + '@example.com', password: 'e2e-only-password', name: '테스트 회원',
    nickname: '테' + id, generation: 14, part }
}

type Account = ReturnType<typeof newAccount>
export const uniqueTitle = () => 'E2E-' + randomUUID().slice(0, 10)

export async function expectApiError(response: APIResponse, status: number, code: string) {
  expect(response.status()).toBe(status)
  expect(await response.json()).toMatchObject({ success: false, status, code })
}

async function data<T>(response: APIResponse, status: number): Promise<T> {
  expect(response.status()).toBe(status)
  const body = await response.json()
  expect(body).toMatchObject({ success: true, status })
  return body.data as T
}

export async function signupApi(request: APIRequestContext, account: Account) {
  return data<{ userId: number }>(await request.post('/api/auth/signup', { data: account }), 201)
}

export async function register(request: APIRequestContext, part?: Account['part']) {
  const account = newAccount(part)
  await signupApi(request, account)
  const { email, password } = account
  const result = await data<{ accessToken: string; tokenType: string }>(
    await request.post('/api/auth/login', { data: { email, password } }), 200)
  expect(result.tokenType).toBe('Bearer')
  return { account, headers: { Authorization: 'Bearer ' + result.accessToken } }
}

export async function createPost(request: APIRequestContext, headers: Record<string, string>, title = uniqueTitle(),
  content = '스터디 테스트 본문') {
  return (await data<{ postId: number }>(await request.post('/api/study-posts', {
    headers, data: { title, content },
  }), 201)).postId
}

export async function createComment(request: APIRequestContext, headers: Record<string, string>, postId: number, content: string) {
  return (await data<{ commentId: number }>(await request.post('/api/study-posts/' + postId + '/comments', {
    headers, data: { content },
  }), 201)).commentId
}

export async function fillSignup(page: Page, account: Account, generation = account.generation) {
  await page.goto('/signup')
  await expect(page.getByRole('heading', { name: '회원가입' })).toBeVisible()
  await page.getByLabel('이메일').fill(account.email)
  await page.getByLabel('비밀번호').fill(account.password)
  await page.getByLabel('이름', { exact: true }).fill(account.name)
  await page.getByLabel('닉네임').fill(account.nickname)
  await page.getByLabel('기수').fill(String(generation))
  await page.getByLabel('파트').selectOption(account.part)
}

export async function loginUi(page: Page, account: Account) {
  await page.goto('/login')
  await expect(page.getByRole('heading', { name: '로그인' })).toBeVisible()
  await page.getByLabel('이메일').fill(account.email)
  await page.getByLabel('비밀번호').fill(account.password)
  await page.getByRole('button', { name: '로그인', exact: true }).click()
  await expect(page.getByRole('heading', { name: '내 정보' })).toBeVisible()
  await expect(page.getByRole('navigation', { name: '주 메뉴' }).getByText(account.nickname, { exact: true })).toBeVisible()
}
