import { expect, test, type Page } from '@playwright/test'
import {
  API_URL,
  bearer,
  closePostViaApi,
  createCommentViaApi,
  createPostViaApi,
  createUserWithToken,
  expectError,
  loginViaUi,
  uniqueTag,
} from './helpers.ts'

function commentList(page: Page) {
  return page.getByRole('list', { name: '댓글 목록' })
}

test.describe('댓글(스터디 신청)', () => {
  test('댓글을 작성하고 수정하고 삭제한다', async ({ page, request }) => {
    const owner = await createUserWithToken(request, 'BE')
    const commenter = await createUserWithToken(request, 'DE')
    const tag = uniqueTag()
    const postId = await createPostViaApi(request, owner.token, { title: `E2E 댓글 ${tag}`, content: '내용' })
    const content = `참여하고 싶어요 ${tag}`
    const edited = `수정한 신청 메시지 ${tag}`

    await loginViaUi(page, commenter.user)
    await page.goto(`/studies/${postId}`)
    await expect(page.getByText('아직 댓글이 없어요.')).toBeVisible()

    // 작성
    await page.getByLabel('댓글 내용').fill(content)
    await page.getByRole('button', { name: '댓글 작성' }).click()
    const item = commentList(page).getByRole('listitem').filter({ hasText: content })
    await expect(item).toBeVisible()
    await expect(item.getByText(`${commenter.user.nickname} · 14기 · DE(디자인)`)).toBeVisible()
    await expect(page.getByLabel('댓글 내용')).toHaveValue('')

    // 수정
    await item.getByRole('button', { name: '수정' }).click()
    await page.getByLabel('댓글 수정').fill(edited)
    await page.getByRole('button', { name: '저장' }).click()
    const editedItem = commentList(page).getByRole('listitem').filter({ hasText: edited })
    await expect(editedItem).toBeVisible()
    await expect(page.getByText(content, { exact: true })).toHaveCount(0)

    // 삭제
    await editedItem.getByRole('button', { name: '삭제' }).click()
    await editedItem.getByRole('group', { name: '댓글 삭제 확인' }).getByRole('button', { name: '삭제 확인' }).click()
    await expect(page.getByText('아직 댓글이 없어요.')).toBeVisible()
    await expect(page.getByText(edited)).toHaveCount(0)
  })

  test('모집이 마감된 글에 댓글을 쓰면 서버 에러 메시지를 보여주고, API는 409를 내려준다', async ({
    page,
    request,
  }) => {
    const owner = await createUserWithToken(request, 'BE')
    const commenter = await createUserWithToken(request, 'FE')
    const postId = await createPostViaApi(request, owner.token, { title: `E2E 마감 ${uniqueTag()}`, content: '내용' })
    await closePostViaApi(request, owner.token, postId)

    await loginViaUi(page, commenter.user)
    await page.goto(`/studies/${postId}`)
    await expect(page.getByRole('article').getByText('모집 마감')).toBeVisible()
    await page.getByLabel('댓글 내용').fill('늦었지만 참여하고 싶어요')
    await page.getByRole('button', { name: '댓글 작성' }).click()

    await expect(page.getByRole('alert')).toHaveText('모집이 마감된 글에는 댓글을 쓸 수 없어요.')
    await expect(page.getByText('아직 댓글이 없어요.')).toBeVisible()

    const response = await request.post(`${API_URL}/api/study-posts/${postId}/comments`, {
      data: { content: 'API로 직접 신청' },
      headers: bearer(commenter.token),
    })
    await expectError(response, 409, 'STUDY-003')
  })

  test('남의 댓글에는 수정, 삭제 버튼이 없고, API로 수정하거나 삭제하면 403이다', async ({ page, request }) => {
    const owner = await createUserWithToken(request, 'BE')
    const commenter = await createUserWithToken(request, 'FE')
    const other = await createUserWithToken(request, 'PM')
    const tag = uniqueTag()
    const postId = await createPostViaApi(request, owner.token, { title: `E2E 남의 댓글 ${tag}`, content: '내용' })
    const content = `다른 사람의 신청 ${tag}`
    const commentId = await createCommentViaApi(request, commenter.token, postId, content)

    await loginViaUi(page, other.user)
    await page.goto(`/studies/${postId}`)
    const item = commentList(page).getByRole('listitem').filter({ hasText: content })
    await expect(item).toBeVisible()
    await expect(item.getByRole('button', { name: '수정' })).toHaveCount(0)
    await expect(item.getByRole('button', { name: '삭제' })).toHaveCount(0)

    const patch = await request.patch(`${API_URL}/api/comments/${commentId}`, {
      data: { content: '바꿔치기' },
      headers: bearer(other.token),
    })
    await expectError(patch, 403, 'COMMENT-002')
    const remove = await request.delete(`${API_URL}/api/comments/${commentId}`, { headers: bearer(other.token) })
    await expectError(remove, 403, 'COMMENT-002')

    // 댓글은 그대로 남아 있다
    const list = await request.get(`${API_URL}/api/study-posts/${postId}/comments`)
    const body = await list.json()
    expect(body.data.content).toHaveLength(1)
    expect(body.data.content[0]).toMatchObject({ commentId, content, isAuthor: false })
  })
})
