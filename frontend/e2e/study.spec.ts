import { expect, test, type Page } from '@playwright/test'

function uniqueUser() {
    const id = `${Date.now()}${Math.floor(Math.random() * 1000)}`
    return {
        email: `e2e-${id}@gmail.com`,
        password: `Pass${id}a`,
        name: '테스트',
        nickname: `닉${id.slice(-4)}`,
    }
}

async function signupAndLogin(page: Page, user: ReturnType<typeof uniqueUser>) {
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
    await expect(page.getByRole('button', { name: '로그아웃' })).toBeVisible()
}

test('모집글 작성, 검색, 수정, 마감, 삭제', async ({ page }) => {
    test.setTimeout(60000)
    const user = uniqueUser()
    const title = `알고리즘-${Date.now()}`
    await signupAndLogin(page, user)

    await page.goto('/studies/new')
    await page.getByLabel('제목').fill(title)
    await page.getByLabel('내용').fill('주 2회 백준 풀이')
    await page.getByRole('button', { name: '작성' }).click()
    await expect(page).toHaveURL(/\/studies\/\d+/)
    await expect(page.getByRole('heading', { name: title })).toBeVisible()

    await page.getByRole('link', { name: '코테이토 스터디' }).click()
    await page.getByLabel('검색').fill(title)
    await page.getByRole('button', { name: '검색' }).click()
    await expect(page.getByRole('link', { name: title })).toBeVisible()

    await page.getByLabel('모집 상태').selectOption('RECRUITING')
    await page.getByLabel('파트').selectOption('BE')
    await page.getByRole('button', { name: '검색' }).click()
    await expect(page.getByRole('link', { name: title })).toBeVisible()

    await page.getByRole('link', { name: title }).click()
    await page.getByRole('link', { name: '수정' }).click()
    await expect(page.getByLabel('제목')).toHaveValue(title)
    await page.getByLabel('제목').fill(`${title}-수정`)
    await page.getByRole('button', { name: '수정' }).click()
    await expect(page).toHaveURL(/\/studies\/\d+$/)
    await expect(page.locator('h1')).toContainText(`${title}-수정`)

    await page.getByRole('button', { name: '모집 마감' }).click()
    await expect(page.getByText('CLOSED')).toBeVisible()
    await expect(page.getByRole('button', { name: '댓글 작성' })).toHaveCount(0)

    await page.getByRole('button', { name: '삭제' }).click()
    await expect(page).toHaveURL(/\/studies/)
    await expect(page.getByRole('link', { name: `${title}-수정` })).toHaveCount(0)
})

test('로그인 없이 작성 불가, 남의 글 수정 불가', async ({ page }) => {
    test.setTimeout(60000)
    const owner = uniqueUser()
    const other = uniqueUser()
    const title = `남의글-${Date.now()}`

    await signupAndLogin(page, owner)
    await page.goto('/studies/new')
    await page.getByLabel('제목').fill(title)
    await page.getByLabel('내용').fill('내용입니다')
    await page.getByRole('button', { name: '작성' }).click()
    await expect(page).toHaveURL(/\/studies\/\d+/)
    const url = page.url()

    await page.getByRole('button', { name: '로그아웃' }).click()
    await page.goto('/studies/new')
    await expect(page).toHaveURL(/\/login/)

    await signupAndLogin(page, other)
    await page.goto(url)
    await expect(page.getByRole('heading', { name: title })).toBeVisible()
    await expect(page.getByRole('link', { name: '수정' })).toHaveCount(0)
    await expect(page.getByRole('button', { name: '삭제' })).toHaveCount(0)
})

test('댓글 작성 후 삭제, 마감 글에는 작성 불가', async ({ page }) => {
    test.setTimeout(60000)
    const user = uniqueUser()
    const title = `댓글글-${Date.now()}`
    await signupAndLogin(page, user)
    await page.goto('/studies/new')
    await page.getByLabel('제목').fill(title)
    await page.getByLabel('내용').fill('댓글 테스트')
    await page.getByRole('button', { name: '작성' }).click()
    await expect(page).toHaveURL(/\/studies\/\d+/)

    await page.getByLabel('댓글').fill('참여하고 싶어요')
    await page.getByRole('button', { name: '댓글 작성' }).click()
    await expect(page.getByText('참여하고 싶어요')).toBeVisible()
    await page.getByRole('button', { name: '댓글 삭제' }).click()
    await expect(page.getByText('참여하고 싶어요')).toHaveCount(0)

    await page.getByRole('button', { name: '모집 마감' }).click()
    await expect(page.getByText('CLOSED')).toBeVisible()
    await expect(page.getByRole('button', { name: '댓글 작성' })).toHaveCount(0)
})