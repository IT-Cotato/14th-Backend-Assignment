import { test, expect } from '@playwright/test'
import { account, apiUrl, bearer, fillSignup, login, register, tokenFor } from './fixtures'

test('회원가입 → 로그인 → 마이페이지 → 로그아웃 및 토큰 무효화', async ({ page, request }) => {
  const data = account()
  await fillSignup(page, data)
  await expect(page).toHaveURL(/\/login$/)
  const loginResponse = page.waitForResponse(response => response.url() === apiUrl('/auth/login') && response.status() === 200)
  await login(page, data)
  const token = (await (await loginResponse).json()).data.accessToken
  await expect(page.getByRole('navigation').getByText(data.nickname)).toBeVisible()
  await page.getByRole('link', { name: '마이페이지' }).click()
  await expect(page.getByText('14기', { exact: true })).toBeVisible()
  await expect(page.getByText('BE', { exact: true })).toBeVisible()
  await page.getByRole('button', { name: '로그아웃' }).click()
  await expect(page).toHaveURL(/\/login$/)
  const response = await request.get(apiUrl('/users/me'), { headers: bearer(token) })
  expect(response.status()).toBe(401)
  expect((await response.json()).code).toBe('COMMON-004')
})

test('중복 이메일과 기수 0 입력을 거부한다', async ({ page, request }) => {
  const data = await register(request)
  await fillSignup(page, data)
  await expect(page.getByRole('alert')).toHaveText('이미 가입된 이메일이에요.')
  const invalid = { ...account(), generation: 0 }
  await fillSignup(page, invalid)
  await expect(page.getByRole('alert')).toHaveText('기수는 1 이상이어야 해요.')
  const response = await request.post(apiUrl('/auth/signup'), { data: invalid })
  expect(response.status()).toBe(400)
  expect((await response.json()).code).toBe('COMMON-003')
})

test('틀린 비밀번호와 비로그인 마이페이지 접근을 처리한다', async ({ page, request }) => {
  const data = await register(request)
  await page.goto('/mypage')
  await expect(page).toHaveURL(/\/login$/)
  await page.getByLabel('이메일').fill(data.email)
  await page.getByLabel('비밀번호').fill(account().password)
  await page.getByRole('button', { name: '로그인', exact: true }).click()
  await expect(page.getByRole('alert')).toHaveText('이메일 또는 비밀번호가 틀렸어요.')
})

test('서버에서 무효화한 토큰을 감지하면 로그인으로 이동한다', async ({ page, request }) => {
  const data = await register(request)
  const token = await tokenFor(request, data)
  await page.addInitScript(value => sessionStorage.setItem('accessToken', value), token)
  expect((await request.post(apiUrl('/auth/logout'), { headers: bearer(token) })).status()).toBe(204)
  await page.goto('/mypage')
  await expect(page).toHaveURL(/\/login$/)
})
