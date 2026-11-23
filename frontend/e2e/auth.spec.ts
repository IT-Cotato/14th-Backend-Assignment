import { expect, test } from '@playwright/test'

function uniqueUser() {
    const id = Date.now()
    return {
        email: `e2e-${id}@gmail.com`,
        password: `Pass${id}a`,
        name: '테스트',
        nickname: `닉${String(id).slice(-4)}`,
    }
}

test('회원가입 후 로그인, 마이페이지, 로그아웃', async ({ page }) => {
    const user = uniqueUser()
    await page.goto('/signup')
    await page.getByLabel('이메일').fill(user.email)
    await page.getByLabel('비밀번호').fill(user.password)
    await page.getByLabel('이름').fill(user.name)
    await page.getByLabel('닉네임').fill(user.nickname)
    await page.getByLabel('기수').fill('14')
    await page.getByLabel('파트').selectOption('BE')
    await page.getByRole('button', { name: '회원가입' }).click()
    await expect(page).toHaveURL(/\/login/)

    await page.getByLabel('이메일').fill(user.email)
    await page.getByLabel('비밀번호').fill(user.password)
    await page.getByRole('button', { name: '로그인' }).click()
    await expect(page.getByText(user.nickname)).toBeVisible()

    await page.getByRole('link', { name: '마이페이지' }).click()
    await expect(page.getByText('기수: 14')).toBeVisible()
    await expect(page.getByText('파트: BE')).toBeVisible()

    await page.getByRole('button', { name: '로그아웃' }).click()
    await expect(page).toHaveURL(/\/login/)
})

test('중복 이메일, 기수 0, 틀린 비밀번호, 비로그인 마이페이지', async ({ page }) => {
    const user = uniqueUser()
    await page.goto('/signup')
    await page.getByLabel('이메일').fill(user.email)
    await page.getByLabel('비밀번호').fill(user.password)
    await page.getByLabel('이름').fill(user.name)
    await page.getByLabel('닉네임').fill(user.nickname)
    await page.getByLabel('기수').fill('14')
    await page.getByRole('button', { name: '회원가입' }).click()
    await expect(page).toHaveURL(/\/login/)

    await page.goto('/signup')
    await page.getByLabel('이메일').fill(user.email)
    await page.getByLabel('비밀번호').fill(user.password)
    await page.getByLabel('이름').fill(user.name)
    await page.getByLabel('닉네임').fill('다른닉네')
    await page.getByLabel('기수').fill('14')
    await page.getByRole('button', { name: '회원가입' }).click()
    await expect(page.getByRole('alert')).toBeVisible()

    await page.getByLabel('기수').fill('0')
    await page.getByRole('button', { name: '회원가입' }).click()
    await expect(page.getByRole('alert')).toBeVisible()

    await page.goto('/login')
    await page.getByLabel('이메일').fill(user.email)
    await page.getByLabel('비밀번호').fill('wrongpass')
    await page.getByRole('button', { name: '로그인' }).click()
    await expect(page.getByRole('alert')).toBeVisible()

    await page.goto('/mypage')
    await expect(page).toHaveURL(/\/login/)
})