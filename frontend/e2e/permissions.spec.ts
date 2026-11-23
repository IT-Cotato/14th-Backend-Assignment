import { expect, test } from '@playwright/test'
import {
  API_URL,
  closePostViaApi,
  createCommentViaApi,
  createPostViaApi,
  loginViaApi,
  newUser,
  signupViaApi,
  uniqueTitle,
} from './helpers.ts'

// 화면에서 버튼을 숨겨도 서버가 정말 막는지, API를 직접 호출해서 확인한다.
test.describe('권한 (API 직접 호출)', () => {
  test('토큰 없이 글이나 댓글을 쓰면 401이 온다', async ({ request }) => {
    const author = newUser()
    await signupViaApi(request, author)
    const postId = await createPostViaApi(request, author, uniqueTitle('토큰없음'))

    const post = await request.post(`${API_URL}/study-posts`, { data: { title: 't', content: 'c' } })
    const comment = await request.post(`${API_URL}/study-posts/${postId}/comments`, { data: { content: 'c' } })

    expect(post.status()).toBe(401)
    expect((await post.json()).code).toBe('COMMON-004')
    expect(comment.status()).toBe(401)
  })

  test('남의 모집글을 수정하거나 삭제하면 403이 오고 글은 그대로다', async ({ request }) => {
    const author = newUser()
    const other = newUser()
    await signupViaApi(request, author)
    await signupViaApi(request, other)
    const title = uniqueTitle('남의글API')
    const postId = await createPostViaApi(request, author, title)
    const headers = { Authorization: `Bearer ${await loginViaApi(request, other)}` }

    const patch = await request.patch(`${API_URL}/study-posts/${postId}`, { data: { title: '해킹' }, headers })
    const del = await request.delete(`${API_URL}/study-posts/${postId}`, { headers })

    expect(patch.status()).toBe(403)
    expect((await patch.json()).code).toBe('STUDY-002')
    expect(del.status()).toBe(403)
    const detail = await request.get(`${API_URL}/study-posts/${postId}`)
    expect((await detail.json()).data.title).toBe(title)
  })

  test('남의 댓글을 수정하거나 삭제하면 403이 오고 댓글은 그대로다', async ({ request }) => {
    const author = newUser()
    const other = newUser()
    await signupViaApi(request, author)
    await signupViaApi(request, other)
    const postId = await createPostViaApi(request, author, uniqueTitle('남의댓글API'))
    await createCommentViaApi(request, author, postId, '작성자의 댓글')
    const list = await request.get(`${API_URL}/study-posts/${postId}/comments`)
    const commentId = (await list.json()).data.content[0].commentId
    const headers = { Authorization: `Bearer ${await loginViaApi(request, other)}` }

    const patch = await request.patch(`${API_URL}/comments/${commentId}`, { data: { content: '해킹' }, headers })
    const del = await request.delete(`${API_URL}/comments/${commentId}`, { headers })

    expect(patch.status()).toBe(403)
    expect((await patch.json()).code).toBe('COMMENT-002')
    expect(del.status()).toBe(403)
    const after = await request.get(`${API_URL}/study-posts/${postId}/comments`)
    expect((await after.json()).data.content[0].content).toBe('작성자의 댓글')
  })

  test('마감된 글에 댓글을 쓰면 409가 오고, 다시 열면 쓸 수 있다', async ({ request }) => {
    const author = newUser()
    const other = newUser()
    await signupViaApi(request, author)
    await signupViaApi(request, other)
    const postId = await createPostViaApi(request, author, uniqueTitle('마감API'))
    await closePostViaApi(request, author, postId)
    const otherHeaders = { Authorization: `Bearer ${await loginViaApi(request, other)}` }
    const authorHeaders = { Authorization: `Bearer ${await loginViaApi(request, author)}` }

    const closed = await request.post(`${API_URL}/study-posts/${postId}/comments`, {
      data: { content: '신청할게요' },
      headers: otherHeaders,
    })
    expect(closed.status()).toBe(409)
    expect((await closed.json()).code).toBe('STUDY-003')

    await request.patch(`${API_URL}/study-posts/${postId}`, { data: { studyStatus: 'RECRUITING' }, headers: authorHeaders })
    const reopened = await request.post(`${API_URL}/study-posts/${postId}/comments`, {
      data: { content: '신청할게요' },
      headers: otherHeaders,
    })
    expect(reopened.status()).toBe(201)
  })

  test('로그아웃한 토큰으로 요청하면 401이 온다', async ({ request }) => {
    const user = newUser()
    await signupViaApi(request, user)
    const headers = { Authorization: `Bearer ${await loginViaApi(request, user)}` }
    expect((await request.get(`${API_URL}/users/me`, { headers })).status()).toBe(200)

    const logout = await request.post(`${API_URL}/auth/logout`, { headers })
    expect(logout.status()).toBe(204)

    expect((await request.get(`${API_URL}/users/me`, { headers })).status()).toBe(401)
  })
})
