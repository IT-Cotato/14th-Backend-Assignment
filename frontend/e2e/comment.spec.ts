import { expect, test } from '@playwright/test'

import {
  API_URL,
  closePostByApi,
  createCommentByApi,
  createPostByApi,
  createUserByApi,
  loginByUi,
  uniqueKeyword,
} from './helpers'

test.describe('댓글 (스터디 신청)', () => {
  test('댓글 작성 → 수정 → 삭제', async ({ page, request }) => {
    // 글쓴이와 신청자는 다른 사람이에요
    const owner = await createUserByApi(request)
    const applicant = await createUserByApi(request)
    const postId = await createPostByApi(request, owner.token, {
      title: `${uniqueKeyword()} 댓글 테스트`,
      content: '신청 댓글을 남겨 주세요.',
    })

    await loginByUi(page, applicant.user)
    await page.goto(`/studies/${postId}`)
    await expect(page.getByText('아직 신청 댓글이 없어요.')).toBeVisible()

    // 작성
    const content = '참여하고 싶어요! 매주 나올 수 있어요.'
    await page.getByLabel('신청 메시지').fill(content)
    await page.getByRole('button', { name: '신청하기' }).click()

    const comment = page.getByRole('listitem').filter({ hasText: content })
    await expect(comment).toBeVisible()
    await expect(comment.getByText(applicant.user.nickname)).toBeVisible()
    await expect(page.getByRole('heading', { name: '신청 댓글 1개' })).toBeVisible()
    // 작성하면 입력창이 비워져요
    await expect(page.getByLabel('신청 메시지')).toHaveValue('')

    // 수정
    const editedContent = '일정이 바뀌어서 격주로 참여하고 싶어요.'
    await comment.getByRole('button', { name: '수정' }).click()
    await expect(page.getByLabel('댓글 수정')).toHaveValue(content)
    await page.getByLabel('댓글 수정').fill(editedContent)
    await page.getByRole('button', { name: '저장' }).click()

    const editedComment = page.getByRole('listitem').filter({ hasText: editedContent })
    await expect(editedComment).toBeVisible()
    await expect(page.getByText(content)).toHaveCount(0)

    // 삭제는 한 번 더 확인해요
    await editedComment.getByRole('button', { name: '삭제', exact: true }).click()
    await editedComment.getByRole('button', { name: '삭제 확인' }).click()

    await expect(page.getByText('아직 신청 댓글이 없어요.')).toBeVisible()
    await expect(page.getByRole('heading', { name: '신청 댓글 0개' })).toBeVisible()
  })

  test('모집이 마감된 글은 입력창이 없고, API로 작성해도 409이다', async ({ page, request }) => {
    const owner = await createUserByApi(request)
    const applicant = await createUserByApi(request)
    const postId = await createPostByApi(request, owner.token, {
      title: `${uniqueKeyword()} 마감된 글`,
      content: '이미 마감했어요.',
    })
    await closePostByApi(request, owner.token, postId)

    // 화면: 입력창 대신 안내 문구가 보여요
    await loginByUi(page, applicant.user)
    await page.goto(`/studies/${postId}`)
    await expect(page.getByText('모집이 마감되어 더 이상 신청할 수 없어요.')).toBeVisible()
    await expect(page.getByLabel('신청 메시지')).toHaveCount(0)
    await expect(page.getByRole('button', { name: '신청하기' })).toHaveCount(0)

    // 서버: 화면을 거치지 않고 API로 보내도 409예요
    const response = await request.post(`${API_URL}/api/study-posts/${postId}/comments`, {
      headers: { Authorization: `Bearer ${applicant.token}` },
      data: { content: '마감됐지만 신청할래요.' },
    })
    expect(response.status()).toBe(409)
    expect((await response.json()).code).toBe('STUDY-003')
  })

  test('남의 댓글은 수정·삭제 버튼이 없고, API로 요청해도 403이다', async ({ page, request }) => {
    const writer = await createUserByApi(request)
    const other = await createUserByApi(request)
    const postId = await createPostByApi(request, writer.token, {
      title: `${uniqueKeyword()} 남의 댓글`,
      content: '댓글 작성자만 고칠 수 있어요.',
    })
    const content = '제가 쓴 신청 댓글이에요.'
    const commentId = await createCommentByApi(request, writer.token, postId, content)

    // 화면: 다른 사람으로 로그인하면 버튼이 보이지 않아요
    await loginByUi(page, other.user)
    await page.goto(`/studies/${postId}`)
    const comment = page.getByRole('listitem').filter({ hasText: content })
    await expect(comment).toBeVisible()
    await expect(comment.getByRole('button', { name: '수정' })).toHaveCount(0)
    await expect(comment.getByRole('button', { name: '삭제', exact: true })).toHaveCount(0)

    // 서버: 화면을 거치지 않고 API로 보내도 403이에요
    const headers = { Authorization: `Bearer ${other.token}` }
    const patch = await request.patch(`${API_URL}/api/comments/${commentId}`, {
      headers,
      data: { content: '몰래 바꾼 댓글' },
    })
    expect(patch.status()).toBe(403)
    expect((await patch.json()).code).toBe('COMMENT-002')

    const remove = await request.delete(`${API_URL}/api/comments/${commentId}`, { headers })
    expect(remove.status()).toBe(403)
    expect((await remove.json()).code).toBe('COMMENT-002')

    // 댓글은 그대로 남아 있어요
    await page.reload()
    await expect(page.getByRole('listitem').filter({ hasText: content })).toBeVisible()
  })

  test('빈 댓글은 보내지 않고 에러 메시지가 보인다', async ({ page, request }) => {
    const owner = await createUserByApi(request)
    const applicant = await createUserByApi(request)
    const postId = await createPostByApi(request, owner.token, {
      title: `${uniqueKeyword()} 빈 댓글`,
      content: '빈 댓글은 안 돼요.',
    })

    await loginByUi(page, applicant.user)
    await page.goto(`/studies/${postId}`)
    await expect(page.getByText('아직 신청 댓글이 없어요.')).toBeVisible()

    // 공백만 넣어도 빈 댓글이에요
    await page.getByLabel('신청 메시지').fill('   ')
    await page.getByRole('button', { name: '신청하기' }).click()

    await expect(page.getByRole('alert')).toHaveText('댓글은 1~500자로 입력해 주세요.')
    await expect(page.getByText('아직 신청 댓글이 없어요.')).toBeVisible()
  })
})
