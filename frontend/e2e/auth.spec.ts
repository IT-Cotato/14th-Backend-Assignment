import { expect, test } from '@playwright/test'
import { API_URL, authHeader, createLoggedInUser, loginViaUi, newUser, signupViaApi } from './helpers'

test.describe('인증', () => {
  test('기수와 파트를 입력해 가입하고 로그인하면 헤더와 마이페이지에 정보가 보이고 로그아웃할 수 있다', async ({
    page,
  }) => {
    const user = newUser({ generation: 13, part: 'FE' })

    await page.goto('/signup')
    await page.getByLabel('이메일').fill(user.email)
    await page.getByLabel('비밀번호').fill(user.password)
    await page.getByLabel('이름').fill(user.name)
    await page.getByLabel('닉네임').fill(user.nickname)
    await page.getByLabel('기수').fill('13')
    await page.getByLabel('파트').selectOption('FE')
    await page.getByRole('button', { name: '가입하기' }).click()

    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByText('회원가입이 완료됐어요. 로그인해 주세요.')).toBeVisible()

    await page.getByLabel('이메일').fill(user.email)
    await page.getByLabel('비밀번호').fill(user.password)
    await page.getByRole('button', { name: '로그인' }).click()

    await expect(page).toHaveURL(/\/studies$/)
    const nav = page.getByRole('navigation', { name: '주요 메뉴' })
    await expect(nav.getByText(`${user.nickname}님`)).toBeVisible()
    await expect(nav.getByRole('link', { name: '로그인' })).toHaveCount(0)

    await nav.getByRole('link', { name: '마이페이지' }).click()
    await expect(page).toHaveURL(/\/mypage$/)
    await expect(page.getByRole('heading', { name: '마이페이지' })).toBeVisible()
    await expect(page.getByText(user.email)).toBeVisible()
    await expect(page.getByText('13기')).toBeVisible()
    await expect(page.getByText('프론트엔드 (FE)')).toBeVisible()

    await nav.getByRole('button', { name: '로그아웃' }).click()
    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByText('로그아웃했어요.')).toBeVisible()
    await expect(nav.getByRole('link', { name: '회원가입' })).toBeVisible()

    await page.goto('/mypage')
    await expect(page).toHaveURL(/\/login$/)
  })

  test('이미 가입된 이메일로 가입하면 서버의 에러 메시지를 보여준다', async ({ page, request }) => {
    const existing = await signupViaApi(request)

    await page.goto('/signup')
    await page.getByLabel('이메일').fill(existing.email)
    await page.getByLabel('비밀번호').fill(existing.password)
    await page.getByLabel('이름').fill('다른사람')
    await page.getByLabel('닉네임').fill('중복테스트')
    await page.getByLabel('기수').fill('14')
    await page.getByLabel('파트').selectOption('BE')
    await page.getByRole('button', { name: '가입하기' }).click()

    await expect(page.getByRole('alert')).toHaveText('이미 가입된 이메일이에요.')
    await expect(page).toHaveURL(/\/signup$/)
  })

  test('기수를 0으로 입력하면 가입되지 않고 서버도 400으로 거절한다', async ({ page, request }) => {
    const user = newUser()

    await page.goto('/signup')
    await page.getByLabel('이메일').fill(user.email)
    await page.getByLabel('비밀번호').fill(user.password)
    await page.getByLabel('이름').fill(user.name)
    await page.getByLabel('닉네임').fill(user.nickname)
    await page.getByLabel('기수').fill('0')
    await page.getByLabel('파트').selectOption('PM')
    await page.getByRole('button', { name: '가입하기' }).click()

    await expect(page.getByText('기수는 1 이상의 정수로 입력해 주세요.')).toBeVisible()
    await expect(page).toHaveURL(/\/signup$/)

    const response = await request.post(`${API_URL}/api/auth/signup`, { data: { ...user, generation: 0 } })
    expect(response.status()).toBe(400)
    expect(await response.json()).toMatchObject({ code: 'COMMON-003', message: '기수는 1 이상의 정수로 입력해 주세요.' })
  })

  test('틀린 비밀번호로 로그인하면 에러 메시지를 보여준다', async ({ page, request }) => {
    const user = await signupViaApi(request)

    await page.goto('/login')
    await page.getByLabel('이메일').fill(user.email)
    await page.getByLabel('비밀번호').fill(`${user.password}-wrong`)
    await page.getByRole('button', { name: '로그인' }).click()

    await expect(page.getByRole('alert')).toHaveText('이메일 또는 비밀번호가 올바르지 않아요.')
    await expect(page).toHaveURL(/\/login$/)
  })

  test('로그인 없이 마이페이지에 들어가면 로그인 화면으로 보내고, 로그인하면 돌아온다', async ({ page, request }) => {
    const user = await signupViaApi(request)

    await page.goto('/mypage')
    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByText('로그인이 필요한 화면이에요.')).toBeVisible()

    await page.getByLabel('이메일').fill(user.email)
    await page.getByLabel('비밀번호').fill(user.password)
    await page.getByRole('button', { name: '로그인' }).click()
    await expect(page).toHaveURL(/\/mypage$/)
  })

  test('로그아웃한 토큰은 서버에서 401로 막힌다', async ({ request }) => {
    const { token } = await createLoggedInUser(request)

    expect((await request.get(`${API_URL}/api/users/me`, { headers: authHeader(token) })).status()).toBe(200)
    expect((await request.post(`${API_URL}/api/auth/logout`, { headers: authHeader(token) })).status()).toBe(204)

    const me = await request.get(`${API_URL}/api/users/me`, { headers: authHeader(token) })
    expect(me.status()).toBe(401)
    expect(await me.json()).toMatchObject({ code: 'COMMON-004' })
  })

  test('토큰이 무효가 되면 로그인 화면으로 보낸다', async ({ page, request }) => {
    const user = await signupViaApi(request)
    await loginViaUi(page, user)

    // 다른 곳에서 같은 토큰이 로그아웃된 상황
    const token = await page.evaluate(() => localStorage.getItem('accessToken'))
    await request.post(`${API_URL}/api/auth/logout`, { headers: authHeader(token!) })

    await page.getByRole('navigation', { name: '주요 메뉴' }).getByRole('link', { name: '마이페이지' }).click()
    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByText('로그인이 만료됐어요. 다시 로그인해 주세요.')).toBeVisible()
    expect(await page.evaluate(() => localStorage.getItem('accessToken'))).toBeNull()
  })
})
