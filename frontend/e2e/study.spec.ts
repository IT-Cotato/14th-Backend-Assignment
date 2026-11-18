import { expect, test } from '@playwright/test'
import {
  callApi,
  closePostByApi,
  createPostByApi,
  createUser,
  errorCodeOf,
  uniqueToken,
} from './helpers/api.ts'
import { loginByUi } from './helpers/ui.ts'

test.describe('스터디 모집글', () => {
  test('로그인하고 모집글을 작성하면 상세와 목록에서 볼 수 있다', async ({ page, request }) => {
    const { user } = await createUser(request)
    const token = uniqueToken()
    const title = `e2e 작성 ${token}`
    const content = `작성 테스트 내용 ${token}`

    await loginByUi(page, user)
    await page.getByRole('link', { name: '모집글 작성' }).click()
    await expect(page).toHaveURL(/\/studies\/new$/)

    await page.getByLabel('제목').fill(title)
    await page.getByLabel('내용').fill(content)
    await page.getByRole('button', { name: '등록' }).click()

    // 등록하면 상세 화면으로 이동한다
    await expect(page).toHaveURL(/\/studies\/\d+$/)
    await expect(page.getByRole('heading', { name: title })).toBeVisible()
    await expect(page.getByText(content)).toBeVisible()

    // 목록에서 검색어로 찾을 수 있다
    await page.getByRole('link', { name: /목록으로/ }).click()
    await page.getByLabel('검색어').fill(token)
    await page.getByRole('button', { name: '검색', exact: true }).click()
    await expect(page.getByRole('link', { name: title })).toBeVisible()
  })

  test('검색어, 모집 상태, 파트로 목록을 걸러 볼 수 있다', async ({ page, request }) => {
    const backend = await createUser(request, { part: 'BE' })
    const frontend = await createUser(request, { part: 'FE' })
    const token = uniqueToken()
    const titleBeOpen = `e2e 필터 ${token}-A`
    const titleFeOpen = `e2e 필터 ${token}-B`
    const titleBeClosed = `e2e 필터 ${token}-C`

    await createPostByApi(request, backend.token, { title: titleBeOpen })
    await createPostByApi(request, frontend.token, { title: titleFeOpen })
    const closedId = await createPostByApi(request, backend.token, { title: titleBeClosed })
    await closePostByApi(request, backend.token, closedId)

    const linkOf = (title: string) => page.getByRole('link', { name: title })

    // 검색어만 넣으면 세 글이 모두 보인다
    await page.goto('/studies')
    await page.getByLabel('검색어').fill(token)
    await page.getByRole('button', { name: '검색', exact: true }).click()
    await expect(linkOf(titleBeOpen)).toBeVisible()
    await expect(linkOf(titleFeOpen)).toBeVisible()
    await expect(linkOf(titleBeClosed)).toBeVisible()

    // 모집 상태: 모집 마감
    await page.getByLabel('모집 상태').selectOption({ label: '모집 마감' })
    await page.getByRole('button', { name: '검색', exact: true }).click()
    await expect(linkOf(titleBeClosed)).toBeVisible()
    await expect(linkOf(titleBeOpen)).toHaveCount(0)
    await expect(linkOf(titleFeOpen)).toHaveCount(0)

    // 모집 상태를 전체로 돌리고 작성자 파트: 프론트엔드
    await page.getByLabel('모집 상태').selectOption({ label: '전체' })
    await page.getByLabel('작성자 파트').selectOption({ label: '프론트엔드 (FE)' })
    await page.getByRole('button', { name: '검색', exact: true }).click()
    await expect(linkOf(titleFeOpen)).toBeVisible()
    await expect(linkOf(titleBeOpen)).toHaveCount(0)
    await expect(linkOf(titleBeClosed)).toHaveCount(0)
  })

  test('작성자는 모집글을 수정하고 마감한 뒤 삭제할 수 있다', async ({ page, request }) => {
    const { user, token } = await createUser(request)
    const original = `e2e 수정 전 ${uniqueToken()}`
    const edited = `e2e 수정 후 ${uniqueToken()}`
    const postId = await createPostByApi(request, token, { title: original })

    await loginByUi(page, user)
    await page.goto(`/studies/${postId}`)
    await expect(page.getByRole('heading', { name: original })).toBeVisible()

    // 수정
    await page.getByRole('link', { name: '수정', exact: true }).click()
    await expect(page).toHaveURL(new RegExp(`/studies/${postId}/edit$`))
    await page.getByLabel('제목').fill(edited)
    await page.getByRole('button', { name: '수정 완료' }).click()
    await expect(page).toHaveURL(new RegExp(`/studies/${postId}$`))
    await expect(page.getByRole('heading', { name: edited })).toBeVisible()

    // 모집 마감: 버튼이 "모집 재개"로 바뀌고 서버에도 반영된다
    await page.getByRole('button', { name: '모집 마감', exact: true }).click()
    await expect(page.getByRole('button', { name: '모집 재개', exact: true })).toBeVisible()
    const detail = await callApi(request, 'GET', `/api/study-posts/${postId}`)
    const detailBody = (await detail.json()) as { data: { studyStatus: string } }
    expect(detailBody.data.studyStatus).toBe('CLOSED')

    // 삭제: 확인 단계를 거쳐야 지워진다
    await page.getByRole('button', { name: '삭제', exact: true }).click()
    await expect(page.getByRole('alertdialog', { name: '삭제 확인' })).toBeVisible()
    await page.getByRole('button', { name: '삭제 확인', exact: true }).click()
    await expect(page).toHaveURL(/\/studies$/)

    const gone = await callApi(request, 'GET', `/api/study-posts/${postId}`)
    expect(gone.status()).toBe(404)
    expect(await errorCodeOf(gone)).toBe('STUDY-001')
  })

  test('로그인 없이 모집글을 작성하려 하면 로그인 화면으로 보내고 API도 거부한다', async ({ page, request }) => {
    // 비로그인 화면에는 작성 버튼이 없다
    await page.goto('/studies')
    await expect(page.getByRole('banner').getByRole('link', { name: '로그인' })).toBeVisible()
    await expect(page.getByRole('link', { name: '모집글 작성' })).toHaveCount(0)

    // 주소를 직접 입력해도 로그인 화면으로 이동한다
    await page.goto('/studies/new')
    await expect(page).toHaveURL(/\/login$/)

    // 화면을 거치지 않고 API를 호출해도 401로 거부한다
    const response = await callApi(request, 'POST', '/api/study-posts', { data: { title: '제목', content: '내용' } })
    expect(response.status()).toBe(401)
    expect(await errorCodeOf(response)).toBe('COMMON-004')
  })

  test('남의 모집글은 수정하거나 삭제할 수 없다', async ({ page, request }) => {
    const author = await createUser(request)
    const other = await createUser(request)
    const title = `e2e 남의 글 ${uniqueToken()}`
    const postId = await createPostByApi(request, author.token, { title })

    // 화면: 다른 사람이 보면 관리 버튼이 없고, 수정 주소로 직접 들어가도 막힌다
    await loginByUi(page, other.user)
    await page.goto(`/studies/${postId}`)
    await expect(page.getByRole('heading', { name: title })).toBeVisible()
    await expect(page.getByRole('link', { name: '수정', exact: true })).toHaveCount(0)
    await expect(page.getByRole('button', { name: '삭제', exact: true })).toHaveCount(0)

    await page.goto(`/studies/${postId}/edit`)
    await expect(page.getByText('작성자만 모집글을 수정할 수 있어요.')).toBeVisible()

    // 서버: 버튼을 숨겨도 API를 직접 호출하면 403으로 막는지 확인한다
    const patch = await callApi(request, 'PATCH', `/api/study-posts/${postId}`, {
      token: other.token,
      data: { title: '가로채기' },
    })
    expect(patch.status()).toBe(403)
    expect(await errorCodeOf(patch)).toBe('STUDY-002')

    const remove = await callApi(request, 'DELETE', `/api/study-posts/${postId}`, { token: other.token })
    expect(remove.status()).toBe(403)
    expect(await errorCodeOf(remove)).toBe('STUDY-002')

    // 글은 그대로 남아 있다
    const stillThere = await callApi(request, 'GET', `/api/study-posts/${postId}`)
    expect(stillThere.status()).toBe(200)
  })
})
