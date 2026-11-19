import { expect, test } from '@playwright/test'
import { auth, createPostByApi, createUser, loginByUi, uid } from './helpers'

test.describe('스터디 모집글', () => {
  test('작성 → 목록 → 검색·상태·파트 필터 → 상세 → 수정 → 모집 마감 → 삭제', async ({ page, request }) => {
    const { user } = await createUser(request, { part: 'DE', generation: 12 })
    const key = uid()
    const title = `e2e 알고리즘 ${key}`

    await loginByUi(page, user)

    // 작성
    await page.getByRole('link', { name: '모집글 작성' }).click()
    await expect(page).toHaveURL(/\/studies\/new$/)
    await page.getByLabel('제목', { exact: true }).fill(title)
    await page.getByLabel('내용', { exact: true }).fill('주 2회 백준 풀이해요')
    await page.getByRole('button', { name: '작성하기' }).click()
    await expect(page.getByRole('heading', { name: title })).toBeVisible()
    await expect(page.getByText('모집 중')).toBeVisible()

    // 목록 + 키워드 검색 (작성자 닉네임·기수·파트 표시)
    await page.getByRole('link', { name: '← 목록으로' }).click()
    await page.getByLabel('검색어').fill(key)
    await page.getByRole('button', { name: '검색' }).click()
    const item = page.getByRole('listitem').filter({ hasText: title })
    await expect(item).toHaveCount(1)
    await expect(item).toContainText(user.nickname)
    await expect(item).toContainText('12기')
    await expect(item).toContainText('DE')

    // 모집 상태 필터
    await page.getByLabel('모집 상태').selectOption('CLOSED')
    await page.getByRole('button', { name: '검색' }).click()
    await expect(page.getByText('조건에 맞는 모집글이 없어요.')).toBeVisible()
    await page.getByLabel('모집 상태').selectOption('RECRUITING')
    await page.getByRole('button', { name: '검색' }).click()
    await expect(page.getByRole('link', { name: title })).toBeVisible()

    // 파트 필터 (작성자 파트)
    await page.getByLabel('파트').selectOption('BE')
    await page.getByRole('button', { name: '검색' }).click()
    await expect(page.getByText('조건에 맞는 모집글이 없어요.')).toBeVisible()
    await page.getByLabel('파트').selectOption('DE')
    await page.getByRole('button', { name: '검색' }).click()

    // 상세
    await page.getByRole('link', { name: title }).click()
    await expect(page.getByRole('heading', { name: title })).toBeVisible()
    await expect(page.getByText('주 2회 백준 풀이해요')).toBeVisible()

    // 수정
    const newTitle = `${title} 수정`
    await page.getByRole('link', { name: '수정', exact: true }).click()
    await expect(page).toHaveURL(/\/edit$/)
    await expect(page.getByLabel('제목', { exact: true })).toHaveValue(title)
    await page.getByLabel('제목', { exact: true }).fill(newTitle)
    await page.getByLabel('내용', { exact: true }).fill('주 3회로 바꿨어요')
    await page.getByRole('button', { name: '수정하기' }).click()
    await expect(page.getByRole('heading', { name: newTitle })).toBeVisible()
    await expect(page.getByText('주 3회로 바꿨어요')).toBeVisible()

    // 모집 마감
    await page.getByRole('button', { name: '모집 마감' }).click()
    await expect(page.getByText('모집 마감', { exact: true }).first()).toBeVisible()
    await expect(page.getByRole('button', { name: '모집 재개' })).toBeVisible()
    await expect(page.getByText('모집이 마감되어 댓글을 쓸 수 없어요.')).toBeVisible()

    // 삭제
    page.once('dialog', (dialog) => dialog.accept())
    await page.getByRole('button', { name: '삭제', exact: true }).click()
    await expect(page).toHaveURL(/\/studies$/)
    await page.getByLabel('검색어').fill(key)
    await page.getByRole('button', { name: '검색' }).click()
    await expect(page.getByText('조건에 맞는 모집글이 없어요.')).toBeVisible()
  })

  test('로그인 없이 작성하려 하면 로그인 화면으로 보내고, API도 401을 준다', async ({ page, request }) => {
    await page.goto('/studies')
    await expect(page.getByRole('link', { name: '모집글 작성' })).toHaveCount(0)

    await page.goto('/studies/new')
    await expect(page).toHaveURL(/\/login$/)

    const res = await request.post('/api/study-posts', { data: { title: '무단 작성', content: '내용' } })
    expect(res.status()).toBe(401)
  })

  test('남의 글은 수정·삭제 버튼이 없고, API로 시도해도 403이 온다', async ({ page, request }) => {
    const owner = await createUser(request)
    const other = await createUser(request)
    const { postId, title } = await createPostByApi(request, owner.token)

    await loginByUi(page, other.user)
    await page.goto(`/studies/${postId}`)
    await expect(page.getByRole('heading', { name: title })).toBeVisible()
    await expect(page.getByRole('link', { name: '수정', exact: true })).toHaveCount(0)
    await expect(page.getByRole('button', { name: '삭제', exact: true })).toHaveCount(0)
    await expect(page.getByRole('button', { name: '모집 마감' })).toHaveCount(0)

    const patch = await request.patch(`/api/study-posts/${postId}`, { headers: auth(other.token), data: { title: '해킹' } })
    expect(patch.status()).toBe(403)
    expect((await patch.json()).code).toBe('STUDY-002')

    const del = await request.delete(`/api/study-posts/${postId}`, { headers: auth(other.token) })
    expect(del.status()).toBe(403)

    const detail = await request.get(`/api/study-posts/${postId}`)
    expect((await detail.json()).data.title).toBe(title)
  })

  test('없는 모집글에 들어가면 에러 메시지를 보여준다', async ({ page }) => {
    await page.goto('/studies/99999999')
    await expect(page.getByRole('alert')).toHaveText('존재하지 않는 스터디 모집글이에요.')
  })
})
