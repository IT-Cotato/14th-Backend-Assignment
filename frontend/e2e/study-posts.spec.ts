import { expect, test, type Page } from '@playwright/test'
import {
  API_URL,
  bearer,
  createPostViaApi,
  createUserWithToken,
  expectError,
  loginViaUi,
  uniqueTag,
} from './helpers.ts'

// 목록 화면에서 검색어, 모집 상태, 파트로 검색합니다. 빈 값은 "전체"입니다.
async function search(page: Page, filters: { keyword: string; status?: string; part?: string }) {
  await page.getByLabel('검색어').fill(filters.keyword)
  await page.getByLabel('모집 상태').selectOption(filters.status ?? '')
  await page.getByLabel('파트').selectOption(filters.part ?? '')
  await page.getByRole('button', { name: '검색' }).click()
}

function postList(page: Page) {
  return page.getByRole('list', { name: '모집글 목록' })
}

test.describe('스터디 모집글', () => {
  test('작성 → 목록 → 검색과 필터 → 상세 → 수정 → 모집 마감 → 삭제', async ({ page, request }) => {
    const { user } = await createUserWithToken(request, 'BE')
    const tag = uniqueTag()
    const title = `E2E 알고리즘 스터디 ${tag}`
    const editedTitle = `E2E 수정한 스터디 ${tag}`
    await loginViaUi(page, user)

    // 작성
    await page.getByRole('link', { name: '모집글 쓰기' }).click()
    await expect(page).toHaveURL(/\/studies\/new$/)
    await page.getByLabel('제목').fill(title)
    await page.getByLabel('내용', { exact: true }).fill(`주 2회 백준 풀이 ${tag}`)
    await page.getByRole('button', { name: '작성하기' }).click()

    await expect(page).toHaveURL(/\/studies\/\d+$/)
    await expect(page.getByRole('heading', { level: 1, name: title })).toBeVisible()
    const postId = Number(new URL(page.url()).pathname.split('/').pop())

    // 목록 + 검색: 작성자의 닉네임, 기수, 파트가 보인다
    await page.getByRole('link', { name: '← 목록으로' }).click()
    await search(page, { keyword: tag })
    await expect(postList(page).getByRole('listitem')).toHaveCount(1)
    const item = postList(page).getByRole('listitem').filter({ hasText: title })
    await expect(item.getByRole('link', { name: title })).toBeVisible()
    await expect(item.getByText(`${user.nickname} · 14기 · BE(백엔드)`)).toBeVisible()
    await expect(item.getByText('모집 중')).toBeVisible()

    // 필터 조합: 모두 만족하는 글만 나온다
    await search(page, { keyword: tag, status: 'RECRUITING', part: 'BE' })
    await expect(postList(page).getByRole('link', { name: title })).toBeVisible()
    await search(page, { keyword: tag, part: 'FE' })
    await expect(page.getByText('조건에 맞는 모집글이 없어요.')).toBeVisible()
    await search(page, { keyword: tag, status: 'CLOSED' })
    await expect(page.getByText('조건에 맞는 모집글이 없어요.')).toBeVisible()

    // 상세
    await search(page, { keyword: tag })
    await postList(page).getByRole('link', { name: title }).click()
    await expect(page).toHaveURL(new RegExp(`/studies/${postId}$`))
    await expect(page.getByText(`주 2회 백준 풀이 ${tag}`)).toBeVisible()

    // 수정
    await page.getByRole('link', { name: '수정' }).click()
    await expect(page).toHaveURL(new RegExp(`/studies/${postId}/edit$`))
    await expect(page.getByLabel('제목')).toHaveValue(title)
    await page.getByLabel('제목').fill(editedTitle)
    await page.getByRole('button', { name: '수정하기' }).click()
    await expect(page).toHaveURL(new RegExp(`/studies/${postId}$`))
    await expect(page.getByRole('heading', { level: 1, name: editedTitle })).toBeVisible()

    // 모집 마감
    await page.getByRole('button', { name: '모집 마감' }).click()
    await expect(page.getByRole('button', { name: '모집 마감' })).toHaveCount(0)
    await expect(page.getByRole('article').getByText('모집 마감')).toBeVisible()

    await page.getByRole('link', { name: '← 목록으로' }).click()
    await search(page, { keyword: tag, status: 'CLOSED', part: 'BE' })
    await expect(postList(page).getByRole('link', { name: editedTitle })).toBeVisible()

    // 삭제
    await postList(page).getByRole('link', { name: editedTitle }).click()
    await page.getByRole('button', { name: '삭제', exact: true }).click()
    await page.getByRole('group', { name: '삭제 확인' }).getByRole('button', { name: '삭제 확인' }).click()
    await expect(page).toHaveURL(/\/studies$/)
    await search(page, { keyword: tag })
    await expect(page.getByText('조건에 맞는 모집글이 없어요.')).toBeVisible()

    const response = await request.get(`${API_URL}/api/study-posts/${postId}`)
    await expectError(response, 404, 'STUDY-001')
  })

  test('목록은 10개씩 최신순으로 나오고 페이지를 이동할 수 있다', async ({ page, request }) => {
    const { token } = await createUserWithToken(request, 'PM')
    const tag = uniqueTag()
    for (let index = 1; index <= 11; index += 1) {
      await createPostViaApi(request, token, { title: `E2E 페이지 ${tag} ${index}번`, content: '내용' })
    }

    await page.goto(`/studies?keyword=${tag}`)
    await expect(page.getByText('전체 11개')).toBeVisible()
    await expect(postList(page).getByRole('listitem')).toHaveCount(10)
    await expect(postList(page).getByRole('listitem').first()).toContainText(`${tag} 11번`)

    await page.getByRole('navigation', { name: '모집글 페이지' }).getByRole('link', { name: '다음' }).click()
    await expect(page).toHaveURL(/page=1/)
    await expect(postList(page).getByRole('listitem')).toHaveCount(1)
    await expect(postList(page).getByRole('listitem')).toContainText(`${tag} 1번`)
  })

  test('잘못된 모집 상태나 파트로 목록을 조회하면 400을 내려준다', async ({ page, request }) => {
    await expectError(await request.get(`${API_URL}/api/study-posts?status=OPEN`), 400, 'COMMON-003')
    await expectError(await request.get(`${API_URL}/api/study-posts?part=XX`), 400, 'COMMON-003')

    // 화면은 서버 메시지를 보여준다
    await page.goto('/studies?part=XX')
    await expect(page.getByRole('alert')).toHaveText('요청 값이 올바르지 않아요.')
  })

  test('로그인하지 않고 작성하려 하면 로그인 화면으로 이동하고, API는 401을 내려준다', async ({ page, request }) => {
    await page.goto('/studies')
    await page.getByRole('link', { name: '모집글 쓰기' }).click()
    await expect(page).toHaveURL(/\/login$/)

    const response = await request.post(`${API_URL}/api/study-posts`, {
      data: { title: `E2E 비로그인 ${uniqueTag()}`, content: '내용' },
    })
    await expectError(response, 401, 'COMMON-004')
  })

  test('남의 글에는 수정, 삭제, 모집 마감 버튼이 없고, API로 수정하거나 삭제하면 403이다', async ({ page, request }) => {
    const owner = await createUserWithToken(request, 'BE')
    const other = await createUserWithToken(request, 'FE')
    const title = `E2E 남의 글 ${uniqueTag()}`
    const postId = await createPostViaApi(request, owner.token, { title, content: '내용' })

    await loginViaUi(page, other.user)
    await page.goto(`/studies/${postId}`)
    await expect(page.getByRole('heading', { level: 1, name: title })).toBeVisible()
    const article = page.getByRole('article')
    await expect(article.getByRole('link', { name: '수정' })).toHaveCount(0)
    await expect(article.getByRole('button', { name: '삭제' })).toHaveCount(0)
    await expect(article.getByRole('button', { name: '모집 마감' })).toHaveCount(0)

    // 주소로 수정 화면에 들어가도 폼이 없다
    await page.goto(`/studies/${postId}/edit`)
    await expect(page.getByRole('alert')).toHaveText('모집글 작성자만 수정할 수 있어요.')
    await expect(page.getByLabel('제목')).toHaveCount(0)

    // 화면을 거치지 않고 API로 보내도 막힌다
    const patch = await request.patch(`${API_URL}/api/study-posts/${postId}`, {
      data: { title: '바꿔치기', studyStatus: 'CLOSED' },
      headers: bearer(other.token),
    })
    await expectError(patch, 403, 'STUDY-002')
    const remove = await request.delete(`${API_URL}/api/study-posts/${postId}`, { headers: bearer(other.token) })
    await expectError(remove, 403, 'STUDY-002')

    const detail = await request.get(`${API_URL}/api/study-posts/${postId}`)
    const body = await detail.json()
    expect(body.data).toMatchObject({ title, studyStatus: 'RECRUITING', isAuthor: false })
  })
})
