import { expect, test } from '@playwright/test'
import { expectApiError, fillSignup, loginUi, newAccount, signupApi } from './fixtures'

test('회원가입, 로그인, 헤더 닉네임, 내 정보, 로그아웃', async ({ page, request }) => {
  const account = newAccount('FE')
  await fillSignup(page, account)
  await page.getByRole('button', { name: '회원가입' }).click()
  await expect(page.getByRole('status').filter({ hasText: '가입이 완료되었습니다' })).toBeVisible()
  await expect(page.getByLabel('이메일')).toHaveValue(account.email)
  await loginUi(page, account)
  const nav = page.getByRole('navigation', { name: '주 메뉴' })
  await nav.getByRole('link', { name: '모집글' }).click()
  await expect(page.getByRole('heading', { name: '스터디 모집글' })).toBeVisible()
  await expect(nav.getByText(account.nickname, { exact: true })).toBeVisible()
  await nav.getByRole('link', { name: '내 정보' }).click()
  const profile = page.getByRole('region', { name: '내 정보' })
  for (const value of [account.email, account.name, account.nickname, String(account.generation), account.part]) {
    await expect(profile.getByText(value, { exact: true })).toBeVisible()
  }
  const token = await page.evaluate(() => sessionStorage.getItem('cotato.accessToken'))
  expect(token).toBeTruthy()
  const response = page.waitForResponse((res) => res.url().endsWith('/api/auth/logout') && res.request().method() === 'POST')
  await nav.getByRole('button', { name: '로그아웃' }).click()
  expect((await response).status()).toBe(204)
  await expect(page).toHaveURL('/login')
  await expect(page.getByText('로그아웃되었습니다.')).toBeVisible()
  await expect(nav.getByText(account.nickname, { exact: true })).toHaveCount(0)
  await expectApiError(await request.get('/api/users/me', { headers: { Authorization: 'Bearer ' + token } }), 401, 'COMMON-004')
})

test('중복 이메일 가입은 409 오류를 표시한다', async ({ page, request }) => {
  const account = newAccount()
  await signupApi(request, account)
  await fillSignup(page, account)
  const response = page.waitForResponse((res) => res.url().endsWith('/api/auth/signup') && res.request().method() === 'POST')
  await page.getByRole('button', { name: '회원가입' }).click()
  const duplicate = await response
  expect(duplicate.status()).toBe(409)
  expect(await duplicate.json()).toMatchObject({ code: 'USER-001' })
  await expect(page.getByRole('alert')).toContainText('이미 가입된 이메일')
})

test('기수 0을 폼과 서버가 모두 거부한다', async ({ page, request }) => {
  const account = newAccount()
  await fillSignup(page, account, 0)
  await page.getByRole('button', { name: '회원가입' }).click()
  await expect(page).toHaveURL('/signup')
  expect(await page.getByLabel('기수').evaluate((input: HTMLInputElement) => input.validity.rangeUnderflow)).toBe(true)
  await expectApiError(await request.post('/api/auth/signup', { data: { ...account, generation: 0 } }),
    400, 'COMMON-003')
})

test('잘못된 비밀번호는 401 오류를 표시한다', async ({ page, request }) => {
  const account = newAccount()
  await signupApi(request, account)
  await page.goto('/login')
  await page.getByLabel('이메일').fill(account.email)
  await page.getByLabel('비밀번호').fill('incorrect-password')
  const response = page.waitForResponse((res) => res.url().endsWith('/api/auth/login'))
  await page.getByRole('button', { name: '로그인', exact: true }).click()
  const denied = await response
  expect(denied.status()).toBe(401)
  expect(await denied.json()).toMatchObject({ code: 'USER-002' })
  await expect(page.getByRole('alert')).toContainText('일치하지 않습니다')
})

test('비로그인 내 정보 접근은 로그인으로 이동한다', async ({ page, request }) => {
  await page.goto('/me')
  await expect(page).toHaveURL('/login')
  await expectApiError(await request.get('/api/users/me'), 401, 'COMMON-004')
})
