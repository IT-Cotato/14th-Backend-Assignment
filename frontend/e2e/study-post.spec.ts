import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'
import {
  API_URL,
  authHeaders,
  createUserWithToken,
  expectApiError,
  loginInBrowser,
} from './helpers/auth'
import { createPostData, createPostViaApi } from './helpers/studyPost'

function studyMenu(page: Page) {
  return page.getByRole('navigation', { name: '스터디 메뉴' })
}

// 로그인 상태가 화면에 반영될 때까지 기다린다 (isAuthor 등 사용자 기준 화면 확인 전)
async function expectLoggedIn(page: Page, nickname: string) {
  await expect(
    page.getByRole('navigation', { name: '사용자 메뉴' }).getByText(nickname, { exact: true }),
  ).toBeVisible()
}

async function search(page: Page, keyword: string) {
  await page.getByLabel('키워드', { exact: true }).fill(keyword)
  await page.getByRole('button', { name: '검색', exact: true }).click()
}

test.describe('스터디 모집글', () => {
  test('작성 → 목록 → 검색, 필터 → 상세 → 수정 → 모집 마감 → 삭제', async ({ page, request }) => {
    const { user, token } = await createUserWithToken(request, { part: 'PM' })
    await loginInBrowser(page, token)
    const post = createPostData()
    const edited = createPostData()
    const results = page.getByRole('main').getByRole('listitem')
    const article = page.getByRole('article')
    let postId = ''

    await test.step('작성', async () => {
      await page.goto('/')
      await expectLoggedIn(page, user.nickname)
      await studyMenu(page).getByRole('link', { name: '글쓰기' }).click()

      await expect(page.getByRole('heading', { name: '모집글 작성' })).toBeVisible()
      await page.getByLabel('제목', { exact: true }).fill(post.title)
      await page.getByLabel('내용', { exact: true }).fill(post.content)
      await page.getByRole('button', { name: '등록', exact: true }).click()

      await expect(page.getByRole('heading', { level: 1, name: post.title })).toBeVisible()
      await expect(article.getByText('모집 중', { exact: true })).toBeVisible()
      postId = new URL(page.url()).pathname.split('/').pop() ?? ''
      expect(postId).toMatch(/^\d+$/)
    })

    await test.step('목록에 보인다', async () => {
      await studyMenu(page).getByRole('link', { name: '스터디 목록' }).click()
      await expect(page.getByRole('link', { name: post.title })).toBeVisible()
    })

    await test.step('키워드 검색', async () => {
      await search(page, post.title)
      await expect(results).toHaveCount(1)
      await expect(results.first()).toContainText(post.title)
    })

    await test.step('모집 상태 필터 (키워드와 함께 적용)', async () => {
      await page.getByLabel('모집 상태', { exact: true }).selectOption('CLOSED')
      await expect(page.getByText('모집글이 없습니다.')).toBeVisible()

      await page.getByLabel('모집 상태', { exact: true }).selectOption('RECRUITING')
      await expect(results).toHaveCount(1)
      await expect(results.first()).toContainText(post.title)
    })

    await test.step('작성자 파트 필터', async () => {
      await page.getByLabel('작성자 파트', { exact: true }).selectOption('FE')
      await expect(page.getByText('모집글이 없습니다.')).toBeVisible()

      await page.getByLabel('작성자 파트', { exact: true }).selectOption('PM')
      await expect(results).toHaveCount(1)
      await expect(results.first()).toContainText(post.title)
    })

    await test.step('상세', async () => {
      await page.getByRole('link', { name: post.title }).click()

      await expect(page.getByRole('heading', { level: 1, name: post.title })).toBeVisible()
      await expect(article.getByText(post.content)).toBeVisible()
      await expect(article.getByText(`${user.nickname} · ${user.generation}기 · PM`)).toBeVisible()
    })

    await test.step('수정', async () => {
      await article.getByRole('link', { name: '수정', exact: true }).click()

      await expect(page.getByRole('heading', { name: '모집글 수정' })).toBeVisible()
      await expect(page.getByLabel('제목', { exact: true })).toHaveValue(post.title)
      await page.getByLabel('제목', { exact: true }).fill(edited.title)
      await page.getByLabel('내용', { exact: true }).fill(edited.content)
      await page.getByRole('button', { name: '수정', exact: true }).click()

      await expect(page).toHaveURL(new RegExp(`/studies/${postId}$`))
      await expect(page.getByRole('heading', { level: 1, name: edited.title })).toBeVisible()
      await expect(article.getByText(edited.content)).toBeVisible()
    })

    await test.step('모집 마감', async () => {
      await article.getByRole('button', { name: '모집 마감', exact: true }).click()

      await expect(article.getByRole('button', { name: '모집 마감', exact: true })).toHaveCount(0)
      await expect(article.getByText('모집 마감', { exact: true })).toBeVisible()
    })

    await test.step('삭제', async () => {
      page.once('dialog', (dialog) => dialog.accept())
      await article.getByRole('button', { name: '삭제', exact: true }).click()

      await expect(page).toHaveURL(/\/studies$/)
      await search(page, edited.title)
      await expect(page.getByText('모집글이 없습니다.')).toBeVisible()

      const response = await request.get(`${API_URL}/api/study-posts/${postId}`)
      await expectApiError(response, 404, 'STUDY-001')
    })
  })

  test('로그인 없이 글쓰기를 누르면 로그인 화면으로 이동한다', async ({ page, request }) => {
    await page.goto('/')
    await studyMenu(page).getByRole('link', { name: '글쓰기' }).click()

    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByRole('heading', { name: '로그인' })).toBeVisible()

    const response = await request.post(`${API_URL}/api/study-posts`, { data: createPostData() })
    await expectApiError(response, 401, 'COMMON-004')
  })

  test('남의 글은 수정할 수 없다', async ({ page, request }) => {
    const author = await createUserWithToken(request)
    const post = await createPostViaApi(request, author.token)
    const other = await createUserWithToken(request)
    await loginInBrowser(page, other.token)

    await page.goto(`/studies/${post.postId}`)
    await expectLoggedIn(page, other.user.nickname)
    const article = page.getByRole('article')
    await expect(page.getByRole('heading', { level: 1, name: post.title })).toBeVisible()
    await expect(article.getByRole('link', { name: '수정', exact: true })).toHaveCount(0)
    await expect(article.getByRole('button', { name: '모집 마감', exact: true })).toHaveCount(0)
    await expect(article.getByRole('button', { name: '삭제', exact: true })).toHaveCount(0)

    await page.goto(`/studies/${post.postId}/edit`)
    await expect(page.getByRole('alert')).toHaveText('작성자만 수정할 수 있습니다.')
    await expect(page.getByLabel('제목', { exact: true })).toHaveCount(0)

    const response = await request.patch(`${API_URL}/api/study-posts/${post.postId}`, {
      headers: authHeaders(other.token),
      data: { title: '남이 바꾼 제목' },
    })
    await expectApiError(response, 403, 'STUDY-002')
  })
})
