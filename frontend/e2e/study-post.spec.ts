import { expect, test, type Page } from '@playwright/test'

import {
  API_URL,
  createPostByApi,
  createUserByApi,
  deletePostByApi,
  loginByUi,
  uniqueKeyword,
} from './helpers'

const EMPTY_LIST = '조건에 맞는 모집글이 없어요.'

// 목록 화면에서 검색 조건을 넣고 검색해요. 빈 문자열은 "전체"예요.
async function search(page: Page, condition: { keyword: string; status?: string; part?: string }) {
  await page.getByLabel('검색어').fill(condition.keyword)
  await page.getByLabel('모집 상태').selectOption(condition.status ?? '')
  await page.getByLabel('파트').selectOption(condition.part ?? '')
  await page.getByRole('button', { name: '검색' }).click()
}

test.describe('스터디 모집글', () => {
  test('작성 → 목록 → 검색·필터 → 상세 → 수정 → 모집 마감 → 삭제', async ({ page, request }) => {
    // 작성자는 BE 파트
    const { user } = await createUserByApi(request, { part: 'BE' })
    await loginByUi(page, user)

    // 제목에 고유한 키워드를 넣어서 내 글만 검색되게 해요
    const keyword = uniqueKeyword()
    const title = `${keyword} 스프링 스터디`
    const editedTitle = `${keyword} JPA 스터디`

    // 작성
    await page.getByRole('link', { name: '모집글 작성' }).click()
    await expect(page).toHaveURL(/\/studies\/new$/)
    await page.getByLabel('제목').fill(title)
    await page.getByLabel('내용').fill('매주 토요일에 모여서 스프링을 공부해요.')
    await page.getByRole('button', { name: '작성하기' }).click()

    // 작성하면 상세 화면으로 가요
    await expect(page).toHaveURL(/\/studies\/\d+$/)
    await expect(page.getByRole('heading', { level: 1, name: title })).toBeVisible()

    // 목록 + 키워드 검색
    await page.goto('/studies')
    await search(page, { keyword })
    const postLink = page.getByRole('link', { name: title })
    await expect(postLink).toBeVisible()
    await expect(page.getByText('총 1개')).toBeVisible()

    // 모집 상태 필터: 새 글은 모집 중이에요
    await search(page, { keyword, status: 'CLOSED' })
    await expect(page.getByText(EMPTY_LIST)).toBeVisible()
    await search(page, { keyword, status: 'RECRUITING' })
    await expect(postLink).toBeVisible()

    // 파트 필터: 작성자 파트(BE)로 걸러요
    await search(page, { keyword, part: 'FE' })
    await expect(page.getByText(EMPTY_LIST)).toBeVisible()
    await search(page, { keyword, part: 'BE' })
    await expect(postLink).toBeVisible()

    // 상세
    await postLink.click()
    await expect(page.getByRole('heading', { level: 1, name: title })).toBeVisible()
    await expect(page.getByText('매주 토요일에 모여서 스프링을 공부해요.')).toBeVisible()

    // 수정
    await page.getByRole('link', { name: '수정', exact: true }).click()
    await expect(page).toHaveURL(/\/studies\/\d+\/edit$/)
    await expect(page.getByLabel('제목')).toHaveValue(title)
    await page.getByLabel('제목').fill(editedTitle)
    await page.getByRole('button', { name: '수정하기' }).click()

    await expect(page).toHaveURL(/\/studies\/\d+$/)
    await expect(page.getByRole('heading', { level: 1, name: editedTitle })).toBeVisible()

    // 모집 마감: 버튼이 "모집 재개"로 바뀌고, 신청(댓글)을 더 받지 않아요
    await page.getByRole('button', { name: '모집 마감' }).click()
    await expect(page.getByRole('button', { name: '모집 재개' })).toBeVisible()
    await expect(page.getByText('모집이 마감되어 더 이상 신청할 수 없어요.')).toBeVisible()

    // 삭제는 한 번 더 확인해요
    await page.getByRole('button', { name: '삭제', exact: true }).click()
    await page.getByRole('button', { name: '삭제 확인' }).click()
    await expect(page).toHaveURL(/\/studies$/)

    // 삭제한 글은 목록에서 검색되지 않아요
    await search(page, { keyword })
    await expect(page.getByText(EMPTY_LIST)).toBeVisible()
  })

  test('로그인하지 않고 모집글을 작성하려 하면 로그인 화면으로 이동한다', async ({ page }) => {
    await page.goto('/studies')
    await page.getByRole('link', { name: '모집글 작성' }).click()

    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByRole('heading', { name: '로그인' })).toBeVisible()
  })

  test('남의 글은 수정·삭제·마감 버튼이 없고, API로 요청해도 403이다', async ({ page, request }) => {
    const owner = await createUserByApi(request)
    const other = await createUserByApi(request)
    const title = `${uniqueKeyword()} 남의 글`
    const postId = await createPostByApi(request, owner.token, { title, content: '작성자만 고칠 수 있어요.' })

    // 화면: 다른 사람으로 로그인하면 버튼이 보이지 않아요
    await loginByUi(page, other.user)
    await page.goto(`/studies/${postId}`)
    await expect(page.getByRole('heading', { level: 1, name: title })).toBeVisible()
    await expect(page.getByRole('link', { name: '수정', exact: true })).toHaveCount(0)
    await expect(page.getByRole('button', { name: '모집 마감' })).toHaveCount(0)
    await expect(page.getByRole('button', { name: '삭제', exact: true })).toHaveCount(0)

    // 화면: 수정 주소로 직접 들어가도 막혀요
    await page.goto(`/studies/${postId}/edit`)
    await expect(page.getByRole('alert')).toHaveText('모집글 작성자만 수정할 수 있어요.')

    // 서버: 화면을 거치지 않고 API로 보내도 403이에요
    const headers = { Authorization: `Bearer ${other.token}` }
    const patch = await request.patch(`${API_URL}/api/study-posts/${postId}`, {
      headers,
      data: { title: '몰래 바꾼 제목' },
    })
    expect(patch.status()).toBe(403)
    expect((await patch.json()).code).toBe('STUDY-002')

    const remove = await request.delete(`${API_URL}/api/study-posts/${postId}`, { headers })
    expect(remove.status()).toBe(403)
    expect((await remove.json()).code).toBe('STUDY-002')

    // 글은 그대로 남아 있어요
    const detail = await request.get(`${API_URL}/api/study-posts/${postId}`)
    expect(detail.status()).toBe(200)
    expect((await detail.json()).data.title).toBe(title)
  })

  test('없는 모집글 상세에 들어가면 에러 메시지가 보인다', async ({ page, request }) => {
    // 글을 만들었다가 지워서 확실히 없는 번호를 얻어요
    const owner = await createUserByApi(request)
    const postId = await createPostByApi(request, owner.token, {
      title: `${uniqueKeyword()} 지울 글`,
      content: '곧 지워져요.',
    })
    await deletePostByApi(request, owner.token, postId)

    await page.goto(`/studies/${postId}`)

    await expect(page.getByRole('alert')).toHaveText('존재하지 않는 스터디 모집글이에요.')
    await expect(page.getByRole('link', { name: '목록으로' })).toBeVisible()
  })
})
