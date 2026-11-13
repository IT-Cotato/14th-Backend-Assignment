import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { api, errorMessage, json, parts } from './api'
import type { Part } from './api'
import { useAuth } from './auth'
import { useLoad } from './use-load'

export type Author = { nickname: string; generation: number; part: Part }
export type Page<T> = { content: T[]; page: number; size: number; totalElements: number; totalPages: number }
type Status = 'RECRUITING' | 'CLOSED'
type Summary = { postId: number; title: string; studyStatus: Status; author: Author; commentCount: number; createdAt: string }
type Detail = Omit<Summary, 'commentCount'> & { content: string; isAuthor: boolean; updatedAt: string }

export function AuthorLine({ author }: { author: Author }) {
  return <span className="text-sm text-slate-600">{author.nickname} · {author.generation}기 · {author.part}</span>
}

function StatusOptions() {
  return <><option value="RECRUITING">모집 중</option><option value="CLOSED">모집 마감</option></>
}

export function StudyList() {
  const { token } = useAuth()
  const [params, setParams] = useSearchParams()
  const result = useLoad<Page<Summary>>('/study-posts?' + params)
  function search(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const query = new URLSearchParams()
    new FormData(event.currentTarget).forEach((value, key) => { if (value) query.set(key, String(value)) })
    setParams(query)
  }
  function changePage(page: number) {
    const query = new URLSearchParams(params)
    query.set('page', String(page))
    setParams(query)
  }

  return <section className="space-y-5">
    <div className="flex flex-wrap items-start justify-between gap-3">
      <h1 className="mb-0">스터디 모집</h1>
      <Link className="primary" to={token ? '/studies/new' : '/login'}>모집글 작성</Link>
    </div>
    <form key={params.toString()} onSubmit={search} className="panel grid items-end gap-3 sm:grid-cols-[2fr_1fr_1fr_auto]">
      <label>키워드<input name="keyword" defaultValue={params.get('keyword') ?? ''} placeholder="제목 또는 내용" /></label>
      <label>모집 상태<select name="status" defaultValue={params.get('status') ?? ''}><option value="">전체</option><StatusOptions /></select></label>
      <label>작성자 파트<select name="part" defaultValue={params.get('part') ?? ''}><option value="">전체</option>
        {parts.map(part => <option key={part}>{part}</option>)}</select></label>
      <button className="primary">검색</button>
    </form>
    {result?.error ? <p role="alert">{result.error}</p> : !result?.data ? <p role="status">불러오는 중…</p> : <>
      {result.data.content.length === 0 && <p className="panel">조건에 맞는 모집글이 없어요.</p>}
      <ul className="grid gap-3">
        {result.data.content.map(post => <li key={post.postId} className="panel">
          <div className="flex flex-wrap justify-between gap-2">
            <Link className="text-lg font-bold break-all" to={`/studies/${post.postId}`}>{post.title}</Link>
            <span>{post.studyStatus === 'RECRUITING' ? '모집 중' : '모집 마감'}</span>
          </div>
          <div className="mt-2 flex flex-wrap gap-3"><AuthorLine author={post.author} /><span className="text-sm">댓글 {post.commentCount}</span></div>
        </li>)}
      </ul>
      <div className="flex items-center justify-center gap-3">
        <button disabled={result.data.page === 0} onClick={() => changePage(result.data!.page - 1)}>이전</button>
        <span>{result.data.page + 1} / {Math.max(1, result.data.totalPages)}</span>
        <button disabled={result.data.page + 1 >= result.data.totalPages} onClick={() => changePage(result.data!.page + 1)}>다음</button>
      </div>
    </>}
  </section>
}

export function StudyDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [refresh, setRefresh] = useState(0)
  const result = useLoad<Detail>(`/study-posts/${id}`, refresh)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function update(close: boolean) {
    setBusy(true)
    setError('')
    try {
      if (close) {
        await api(`/study-posts/${id}`, json('PATCH', { studyStatus: 'CLOSED' }))
        setRefresh(value => value + 1)
      } else {
        await api(`/study-posts/${id}`, { method: 'DELETE' })
        navigate('/studies')
      }
    } catch (failure) {
      setError(errorMessage(failure))
    } finally {
      setBusy(false)
    }
  }

  if (result?.error) return <p role="alert">{result.error}</p>
  if (!result?.data) return <p role="status">불러오는 중…</p>
  const post = result.data
  return <div className="space-y-5">
    <Link to="/studies">목록으로</Link>
    <article className="panel space-y-4">
      <h1 className="break-all">{post.title}</h1>
      <div className="flex flex-wrap gap-3"><AuthorLine author={post.author} /><span>{post.studyStatus === 'CLOSED' ? '모집 마감' : '모집 중'}</span></div>
      <p className="whitespace-pre-wrap break-words">{post.content}</p>
      {post.isAuthor && <div className="flex flex-wrap items-center gap-3">
        <Link to={`/studies/${id}/edit`}>글 수정</Link>
        {post.studyStatus === 'RECRUITING' && <button disabled={busy} onClick={() => update(true)}>모집 마감</button>}
        <button disabled={busy} onClick={() => update(false)}>글 삭제</button>
      </div>}
      {error && <p role="alert">{error}</p>}
    </article>
  </div>
}

export function StudyEditor({ edit = false }: { edit?: boolean }) {
  const { id } = useParams()
  return edit ? <ExistingEditor key={id} id={id!} /> : <Editor />
}

function ExistingEditor({ id }: { id: string }) {
  const result = useLoad<Detail>(`/study-posts/${id}`)
  if (result?.error) return <p role="alert">{result.error}</p>
  if (!result?.data) return <p role="status">불러오는 중…</p>
  if (!result.data.isAuthor) return <p role="alert">모집글 작성자만 변경할 수 있어요.</p>
  return <Editor post={result.data} />
}

function Editor({ post }: { post?: Detail }) {
  const navigate = useNavigate()
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setBusy(true)
    setError('')
    const input = Object.fromEntries(new FormData(event.currentTarget))
    try {
      const data = await api<{ postId: number }>(post ? `/study-posts/${post.postId}` : '/study-posts',
        json(post ? 'PATCH' : 'POST', input))
      navigate(`/studies/${data.postId}`)
    } catch (failure) {
      setError(errorMessage(failure))
    } finally {
      setBusy(false)
    }
  }

  return <section className="panel">
    <h1>{post ? '모집글 수정' : '모집글 작성'}</h1>
    <form onSubmit={save} className="grid gap-4">
      <label>제목<input name="title" required maxLength={50} defaultValue={post?.title} /></label>
      <label>내용<textarea name="content" required maxLength={2000} rows={7} defaultValue={post?.content} /></label>
      {post && <label>모집 상태<select name="studyStatus" defaultValue={post.studyStatus}><StatusOptions /></select></label>}
      {error && <p role="alert">{error}</p>}
      <div className="flex items-center gap-3">
        <button className="primary" disabled={busy}>{busy ? '저장 중…' : '저장'}</button>
        <Link to={post ? `/studies/${post.postId}` : '/studies'}>취소</Link>
      </div>
    </form>
  </section>
}
