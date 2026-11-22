import { expect, test } from '@playwright/test'
import { createPost, expectApiError, loginUi, register, uniqueTitle } from './fixtures'

test('모집글 작성, 목록·상세 조회, 수정과 마감, 삭제', async ({ page, request }) => {
  const { account } = await register(request)
  const title = uniqueTitle()
  await loginUi(page, account)
  await page.getByRole('link', { name: '모집글', exact: true }).click()
  await page.getByRole('link', { name: '모집글 작성' }).click()
  await expect(page.getByRole('heading', { name: '모집글 작성' })).toBeVisible()
  await page.getByLabel('제목').fill(title)
  await page.getByLabel('내용', { exact: true }).fill('모집글 원문')
  await page.getByRole('button', { name: '저장' }).click()
  await expect(page.getByRole('heading', { name: title })).toBeVisible()
  const detail = new URL(page.url()).pathname
  await page.getByRole('link', { name: /모집글 목록/ }).click()
  await page.getByRole('link', { name: title }).click()
  await expect(page.getByText('모집글 원문', { exact: true })).toBeVisible()
  await page.getByRole('link', { name: '수정', exact: true }).click()
  await expect(page.getByRole('heading', { name: '모집글 수정' })).toBeVisible()
  await expect(page.getByLabel('제목')).toHaveValue(title)
  await page.getByLabel('내용', { exact: true }).fill('모집글 수정본')
  await page.getByLabel('모집 상태').selectOption('CLOSED')
  await page.getByRole('button', { name: '저장' }).click()
  await expect(page).toHaveURL(detail)
  await expect(page.getByText('모집글 수정본', { exact: true })).toBeVisible()
  await expect(page.getByText('모집 마감', { exact: true })).toBeVisible()
  await page.getByRole('button', { name: '삭제', exact: true }).click()
  await page.getByRole('group', { name: '모집글 삭제 확인' }).getByRole('button', { name: '삭제 확인' }).click()
  await expect(page).toHaveURL('/study-posts')
  await expectApiError(await request.get('/api' + detail), 404, 'STUDY-001')
})

test('공개 검색은 제목·본문을 검색하고 상태·파트를 함께 필터링한다', async ({ page, request }) => {
  const owner = await register(request, 'BE')
  const other = await register(request, 'FE')
  const key = uniqueTitle()
  const title = key + ' 제목 일치'
  const bodyTitle = uniqueTitle()
  await createPost(request, owner.headers, title)
  await createPost(request, owner.headers, bodyTitle, key + ' 본문 일치')
  const closedTitle = key + ' 마감'
  const closed = await createPost(request, owner.headers, closedTitle)
  expect((await request.patch('/api/study-posts/' + closed, {
    headers: owner.headers, data: { studyStatus: 'CLOSED' },
  })).status()).toBe(200)
  const otherTitle = key + ' 다른 파트'
  await createPost(request, other.headers, otherTitle)
  await page.goto('/study-posts')
  await page.getByRole('searchbox').fill(key)
  await page.getByRole('button', { name: '검색' }).click()
  await expect(page.getByText('모집글 4개', { exact: true })).toBeVisible()
  await page.getByLabel('모집 상태').selectOption('RECRUITING')
  await expect(page.getByText('모집글 3개', { exact: true })).toBeVisible()
  await page.getByLabel('작성자 파트').selectOption('BE')
  await expect(page.getByText('모집글 2개', { exact: true })).toBeVisible()
  await expect(page.getByRole('link', { name: bodyTitle, exact: true })).toBeVisible()
  await expect(page.getByRole('link', { name: closedTitle, exact: true })).toHaveCount(0)
  await expect(page.getByRole('link', { name: otherTitle, exact: true })).toHaveCount(0)
  await page.reload()
  await expect(page.getByRole('searchbox')).toHaveValue(key)
  await page.getByRole('link', { name: title, exact: true }).click()
  await expect(page.getByRole('heading', { name: title })).toBeVisible()
  await expect(page.getByRole('link', { name: '수정', exact: true })).toHaveCount(0)
})

test('비로그인 작성은 UI와 API에서 제한된다', async ({ page, request }) => {
  await page.goto('/study-posts/new')
  await expect(page).toHaveURL('/login')
  await expectApiError(await request.post('/api/study-posts', {
    data: { title: uniqueTitle(), content: '보호된 작성 요청' },
  }), 401, 'COMMON-004')
})

test('타인 모집글의 수정 및 삭제가 제한된다', async ({ page, request }) => {
  const owner = await register(request)
  const other = await register(request, 'FE')
  const title = uniqueTitle()
  const id = await createPost(request, owner.headers, title)
  await loginUi(page, other.account)
  await page.goto('/study-posts/' + id)
  await expect(page.getByRole('heading', { name: title })).toBeVisible()
  await expect(page.getByRole('link', { name: '수정', exact: true })).toHaveCount(0)
  await expect(page.getByRole('button', { name: '삭제', exact: true })).toHaveCount(0)
  await page.goto('/study-posts/' + id + '/edit')
  await expect(page.getByRole('alert')).toContainText('모집글 작성자만 수정')
  const path = '/api/study-posts/' + id
  await expectApiError(await request.patch(path, { headers: other.headers, data: { title: '권한 없는 수정' } }), 403, 'STUDY-002')
  await expectApiError(await request.delete(path, { headers: other.headers }), 403, 'STUDY-002')
  expect(await (await request.get(path)).json()).toMatchObject({ data: { title } })
})
