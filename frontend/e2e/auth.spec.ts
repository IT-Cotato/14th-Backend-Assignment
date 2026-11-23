import { expect, test } from '@playwright/test'
import { auth, createUser, loginByUi, newUser, signupByApi } from './helpers'

test.describe('인증', () => {
  test('기수·파트를 입력해 회원가입 → 로그인 → 헤더 닉네임 → 마이페이지 확인 → 로그아웃', async ({ page }) => {
    const user = newUser({ generation: 13, part: 'FE' })

    await page.goto('/signup')
    await page.getByLabel('이메일', { exact: true }).fill(user.email)
    await page.getByLabel('비밀번호', { exact: true }).fill(user.password)
    await page.getByLabel('이름', { exact: true }).fill(user.name)
    await page.getByLabel('닉네임', { exact: true }).fill(user.nickname)
    await page.getByLabel('기수', { exact: true }).fill(String(user.generation))
    await page.getByLabel('파트', { exact: true }).selectOption('FE')
    await page.getByRole('button', { name: '회원가입' }).click()

    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByText('회원가입이 완료됐어요')).toBeVisible()

    await loginByUi(page, user)

    await page.getByRole('link', { name: '마이페이지' }).click()
    await expect(page).toHaveURL(/\/mypage$/)
    await expect(page.getByRole('heading', { name: '마이페이지' })).toBeVisible()
    await expect(page.getByText(user.email)).toBeVisible()
    await expect(page.getByText('13기')).toBeVisible()
    await expect(page.getByText('프론트엔드 (FE)')).toBeVisible()

    await page.getByRole('button', { name: '로그아웃' }).click()
    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByRole('link', { name: '회원가입', exact: true })).toBeVisible()
    await expect(page.getByText(`${user.nickname}님`)).toHaveCount(0)
  })

  test('이미 가입된 이메일로 가입하면 서버 에러 메시지를 보여준다', async ({ page, request }) => {
    const user = newUser()
    await signupByApi(request, user)

    await page.goto('/signup')
    await page.getByLabel('이메일', { exact: true }).fill(user.email)
    await page.getByLabel('비밀번호', { exact: true }).fill(user.password)
    await page.getByLabel('이름', { exact: true }).fill(user.name)
    await page.getByLabel('닉네임', { exact: true }).fill(user.nickname)
    await page.getByLabel('기수', { exact: true }).fill('14')
    await page.getByLabel('파트', { exact: true }).selectOption('BE')
    await page.getByRole('button', { name: '회원가입' }).click()

    await expect(page.getByRole('alert')).toHaveText('이미 가입된 이메일이에요.')
    await expect(page).toHaveURL(/\/signup$/)
  })

  test('기수를 0으로 입력하면 가입되지 않고 에러 메시지를 보여준다', async ({ page }) => {
    const user = newUser()

    await page.goto('/signup')
    await page.getByLabel('이메일', { exact: true }).fill(user.email)
    await page.getByLabel('비밀번호', { exact: true }).fill(user.password)
    await page.getByLabel('이름', { exact: true }).fill(user.name)
    await page.getByLabel('닉네임', { exact: true }).fill(user.nickname)
    await page.getByLabel('기수', { exact: true }).fill('0')
    await page.getByLabel('파트', { exact: true }).selectOption('PM')
    await page.getByRole('button', { name: '회원가입' }).click()

    await expect(page.getByRole('alert')).toHaveText('기수는 1 이상이어야 해요.')
    await expect(page).toHaveURL(/\/signup$/)
  })

  test('틀린 비밀번호로 로그인하면 에러 메시지를 보여준다', async ({ page, request }) => {
    const { user } = await createUser(request)

    await page.goto('/login')
    await page.getByLabel('이메일', { exact: true }).fill(user.email)
    await page.getByLabel('비밀번호', { exact: true }).fill(`${user.password}-wrong`)
    await page.getByRole('button', { name: '로그인' }).click()

    await expect(page.getByRole('alert')).toHaveText('이메일 또는 비밀번호가 올바르지 않아요.')
    await expect(page).toHaveURL(/\/login$/)
  })

  test('로그인 없이 마이페이지에 들어가면 로그인 화면으로 보낸다', async ({ page }) => {
    await page.goto('/mypage')
    await expect(page).toHaveURL(/\/login$/)
  })

  test('로그아웃한 토큰으로 다시 요청하면 401이 온다', async ({ request }) => {
    const { token } = await createUser(request)
    expect((await request.get('/api/users/me', { headers: auth(token) })).status()).toBe(200)

    expect((await request.post('/api/auth/logout', { headers: auth(token) })).status()).toBe(204)

    const res = await request.get('/api/users/me', { headers: auth(token) })
    expect(res.status()).toBe(401)
    expect((await res.json()).code).toBe('COMMON-004')
  })
})
