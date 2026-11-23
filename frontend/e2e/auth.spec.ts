import { expect, test, type Page } from '@playwright/test'

import { API_URL, createUserByApi, loginByUi, newUser, userMenu, type TestUser } from './helpers'

// 회원가입 화면의 입력창을 채워요
async function fillSignupForm(page: Page, user: TestUser, generation: string = String(user.generation)) {
  await page.getByLabel('이메일').fill(user.email)
  await page.getByLabel('비밀번호').fill(user.password)
  await page.getByLabel('이름').fill(user.name)
  await page.getByLabel('닉네임').fill(user.nickname)
  await page.getByLabel('기수').fill(generation)
  await page.getByLabel('파트').selectOption(user.part)
}

test.describe('인증', () => {
  test('회원가입 → 로그인 → 헤더 닉네임 → 마이페이지 기수·파트 확인 → 로그아웃', async ({ page }) => {
    const user = newUser({ generation: 14, part: 'BE' })

    // 회원가입
    await page.goto('/signup')
    await fillSignupForm(page, user)
    await page.getByRole('button', { name: '회원가입' }).click()

    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByText('회원가입이 완료됐어요. 로그인해 주세요.')).toBeVisible()

    // 로그인하면 헤더에 닉네임이 보여요
    await loginByUi(page, user)

    // 마이페이지에서 가입할 때 넣은 기수·파트 확인
    await userMenu(page).getByRole('link', { name: '마이페이지' }).click()
    await expect(page).toHaveURL(/\/mypage$/)
    const main = page.getByRole('main')
    await expect(main.getByText(user.email)).toBeVisible()
    await expect(main.getByText(user.nickname)).toBeVisible()
    await expect(main.getByText('14기')).toBeVisible()
    await expect(main.getByText('BE (백엔드)')).toBeVisible()

    // 로그아웃하면 로그인 화면으로 가고, 헤더가 로그인 전 메뉴로 바뀌어요
    await userMenu(page).getByRole('button', { name: '로그아웃' }).click()
    await expect(page).toHaveURL(/\/login$/)
    await expect(userMenu(page).getByRole('link', { name: '로그인' })).toBeVisible()
    await expect(userMenu(page).getByText(user.nickname)).toHaveCount(0)
  })

  test('이미 가입된 이메일로 가입하면 에러 메시지가 보인다', async ({ page, request }) => {
    const { user: existing } = await createUserByApi(request)
    // 이메일만 같고 나머지는 다른 사람
    const duplicate = newUser({ email: existing.email })

    await page.goto('/signup')
    await fillSignupForm(page, duplicate)
    await page.getByRole('button', { name: '회원가입' }).click()

    await expect(page.getByRole('alert')).toHaveText('이미 가입된 이메일이에요.')
    await expect(page).toHaveURL(/\/signup$/)
  })

  test('기수에 0을 넣으면 가입되지 않고 에러 메시지가 보인다', async ({ page }) => {
    const user = newUser()

    await page.goto('/signup')
    await fillSignupForm(page, user, '0')
    await page.getByRole('button', { name: '회원가입' }).click()

    await expect(page.getByText('기수는 1 이상의 정수여야 해요.')).toBeVisible()
    await expect(page).toHaveURL(/\/signup$/)
  })

  test('비밀번호가 틀리면 로그인되지 않고 에러 메시지가 보인다', async ({ page, request }) => {
    const { user } = await createUserByApi(request)

    await page.goto('/login')
    await page.getByLabel('이메일').fill(user.email)
    // 가입한 비밀번호 뒤에 글자를 더 붙여서 틀린 비밀번호를 만들어요
    await page.getByLabel('비밀번호').fill(`${user.password}x`)
    await page.getByRole('button', { name: '로그인' }).click()

    await expect(page.getByRole('alert')).toHaveText('이메일 또는 비밀번호가 올바르지 않아요.')
    await expect(page).toHaveURL(/\/login$/)
    await expect(userMenu(page).getByRole('link', { name: '로그인' })).toBeVisible()
  })

  test('로그인하지 않고 마이페이지에 들어가면 로그인 화면으로 이동한다', async ({ page }) => {
    await page.goto('/mypage')

    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByRole('heading', { name: '로그인' })).toBeVisible()
  })

  test('로그아웃한 토큰으로는 내 정보 API를 호출할 수 없다 (401)', async ({ page, request }) => {
    const { user } = await createUserByApi(request)
    await loginByUi(page, user)

    // 화면 로그인으로 받은 토큰을 꺼내요
    const token = await page.evaluate(() => localStorage.getItem('accessToken'))
    expect(token).toBeTruthy()
    const headers = { Authorization: `Bearer ${token}` }

    // 로그아웃 전에는 잘 동작해요
    const before = await request.get(`${API_URL}/api/users/me`, { headers })
    expect(before.status()).toBe(200)

    await userMenu(page).getByRole('button', { name: '로그아웃' }).click()
    await expect(page).toHaveURL(/\/login$/)

    // 로그아웃한 토큰은 블랙리스트에 들어가서 401이에요
    const after = await request.get(`${API_URL}/api/users/me`, { headers })
    expect(after.status()).toBe(401)
    expect((await after.json()).code).toBe('COMMON-004')
  })
})
