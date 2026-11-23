import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { api, errorMessage, json } from './api'
import { useAuth } from './auth'
import { AuthorLine } from './studies'
import type { Author, Page } from './studies'
import { useLoad } from './use-load'

type Comment = { commentId: number; content: string; author: Author; isAuthor: boolean; createdAt: string }

export function Comments({ postId, closed }: { postId: string; closed: boolean }) {
  const { token } = useAuth()
  const [page, setPage] = useState(0)
  const [refresh, setRefresh] = useState(0)
  const [editing, setEditing] = useState<number>()
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const path = `/study-posts/${postId}/comments`
  const result = useLoad<Page<Comment>>(`${path}?page=${page}`, refresh)

  async function change(options: RequestInit, target: string, form?: HTMLFormElement) {
    setBusy(true)
    setError('')
    try {
      await api(target, options)
      form?.reset()
      setEditing(undefined)
      if (options.method === 'DELETE' && page > 0 && result?.data?.content.length === 1) {
        setPage(page - 1)
      }
      setRefresh(value => value + 1)
    } catch (failure) {
      setError(errorMessage(failure))
    } finally {
      setBusy(false)
    }
  }

  function submit(event: FormEvent<HTMLFormElement>, id?: number) {
    event.preventDefault()
    const form = event.currentTarget
    const input = Object.fromEntries(new FormData(form))
    void change(json(id ? 'PATCH' : 'POST', input), id ? `/comments/${id}` : path, form)
  }

  return <section className="panel space-y-4" aria-labelledby="comments-heading">
    <h2 id="comments-heading">댓글 · 스터디 신청</h2>
    {closed ? <p>모집이 마감된 글이에요.</p> : token ?
      <form onSubmit={event => submit(event)} className="grid gap-2">
        <label>신청 댓글<textarea name="content" required maxLength={500} rows={3} /></label>
        <button className="primary justify-self-start" disabled={busy}>댓글 등록</button>
      </form> : <Link to="/login">로그인하고 신청하기</Link>}
    {error && <p role="alert">{error}</p>}
    {result?.error ? <p role="alert">{result.error}</p> : !result?.data ? <p role="status">불러오는 중…</p> : <>
      {result.data.content.length === 0 && <p>아직 댓글이 없어요.</p>}
      <ul className="divide-y divide-slate-200">
        {result.data.content.map(comment => <li key={comment.commentId} className="space-y-2 py-3">
          <AuthorLine author={comment.author} />
          {editing === comment.commentId ? <form onSubmit={event => submit(event, comment.commentId)} className="grid gap-2">
            <label>댓글 내용<textarea name="content" defaultValue={comment.content} required maxLength={500} /></label>
            <div className="flex gap-2">
              <button className="primary" disabled={busy}>댓글 저장</button>
              <button type="button" onClick={() => setEditing(undefined)}>취소</button>
            </div>
          </form> : <>
            <p className="whitespace-pre-wrap break-words">{comment.content}</p>
            {comment.isAuthor && <div className="flex gap-2">
              <button disabled={busy} onClick={() => setEditing(comment.commentId)}>댓글 수정</button>
              <button disabled={busy} onClick={() => change({ method: 'DELETE' }, `/comments/${comment.commentId}`)}>댓글 삭제</button>
            </div>}
          </>}
        </li>)}
      </ul>
      {result.data.totalPages > 1 && <div className="flex gap-2">
        <button disabled={page === 0} onClick={() => setPage(page - 1)}>이전 댓글</button>
        <button disabled={page + 1 >= result.data.totalPages} onClick={() => setPage(page + 1)}>다음 댓글</button>
      </div>}
    </>}
  </section>
}
