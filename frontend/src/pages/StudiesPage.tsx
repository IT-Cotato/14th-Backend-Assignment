import { useEffect, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getStudyPosts } from '../api/studyPosts.ts'
import { getErrorMessage } from '../api/client.ts'
import {
  PART_LABELS,
  STATUS_LABELS,
  type Part,
  type PageResponse,
  type StudyPostListItem,
  type StudyStatus,
} from '../api/types.ts'
import { useAuth } from '../auth/useAuth.ts'
import Pagination from '../components/Pagination.tsx'
import StatusBadge from '../components/StatusBadge.tsx'

const PAGE_SIZE = 10

// 검색 조건은 주소(?keyword=...&status=...)에 담아 둔다. 새로고침하거나 뒤로 가도 유지된다.
export default function StudiesPage() {
  const { user } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const keyword = searchParams.get('keyword') ?? ''
  const status = (searchParams.get('status') ?? '') as StudyStatus | ''
  const part = (searchParams.get('part') ?? '') as Part | ''
  const page = Number(searchParams.get('page') ?? '0') || 0

  const [keywordInput, setKeywordInput] = useState(keyword)
  const [result, setResult] = useState<PageResponse<StudyPostListItem> | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    getStudyPosts({
      keyword: keyword || undefined,
      status: status || undefined,
      part: part || undefined,
      page,
      size: PAGE_SIZE,
    })
      .then((data) => {
        if (!cancelled) {
          setResult(data)
          setError('')
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(getErrorMessage(err))
        }
      })
    return () => {
      cancelled = true
    }
  }, [keyword, status, part, page])

  // 조건을 바꾸면 첫 페이지부터 다시 본다
  function updateParams(changes: Record<string, string>) {
    const next = new URLSearchParams(searchParams)
    for (const [key, value] of Object.entries(changes)) {
      if (value) {
        next.set(key, value)
      } else {
        next.delete(key)
      }
    }
    if (!('page' in changes)) {
      next.delete('page')
    }
    setSearchParams(next)
  }

  function handleSearch(e: FormEvent) {
    e.preventDefault()
    updateParams({ keyword: keywordInput.trim() })
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold">스터디 모집글</h1>
        {user ? (
          <Link to="/studies/new" className="rounded bg-indigo-600 px-3 py-1.5 text-sm text-white hover:bg-indigo-700">
            글쓰기
          </Link>
        ) : null}
      </div>

      <form onSubmit={handleSearch} className="flex flex-wrap gap-2 rounded-lg bg-white p-3 shadow-sm">
        <label htmlFor="keyword" className="sr-only">검색어</label>
        <input id="keyword" value={keywordInput} onChange={(e) => setKeywordInput(e.target.value)}
          placeholder="제목이나 내용으로 검색"
          className="min-w-0 flex-1 rounded border border-gray-300 px-3 py-1.5 focus:border-indigo-500 focus:outline-none" />
        <label htmlFor="status" className="sr-only">모집 상태</label>
        <select id="status" value={status} onChange={(e) => updateParams({ status: e.target.value })}
          className="rounded border border-gray-300 px-2 py-1.5">
          <option value="">전체 상태</option>
          {(Object.keys(STATUS_LABELS) as StudyStatus[]).map((s) => (
            <option key={s} value={s}>{STATUS_LABELS[s]}</option>
          ))}
        </select>
        <label htmlFor="part" className="sr-only">작성자 파트</label>
        <select id="part" value={part} onChange={(e) => updateParams({ part: e.target.value })}
          className="rounded border border-gray-300 px-2 py-1.5">
          <option value="">전체 파트</option>
          {(Object.keys(PART_LABELS) as Part[]).map((p) => (
            <option key={p} value={p}>{PART_LABELS[p]}</option>
          ))}
        </select>
        <button type="submit" className="rounded bg-gray-800 px-4 py-1.5 text-sm text-white hover:bg-gray-700">
          검색
        </button>
      </form>

      {error ? <p role="alert" className="text-sm text-red-600">{error}</p> : null}

      {result === null && !error ? <p className="text-gray-500">불러오는 중...</p> : null}

      {result && result.content.length === 0 ? (
        <p className="rounded-lg bg-white p-6 text-center text-gray-500 shadow-sm">조건에 맞는 모집글이 없어요.</p>
      ) : null}

      {result && result.content.length > 0 ? (
        <ul className="space-y-2">
          {result.content.map((post) => (
            <li key={post.postId}>
              <Link to={`/studies/${post.postId}`}
                className="block rounded-lg bg-white p-4 shadow-sm hover:ring-1 hover:ring-indigo-300">
                <div className="flex items-center gap-2">
                  <StatusBadge status={post.studyStatus} />
                  <span className="font-medium">{post.title}</span>
                </div>
                <p className="mt-1 text-sm text-gray-500">
                  {post.author.nickname} · {post.author.generation}기 {PART_LABELS[post.author.part]} ·{' '}
                  {post.createdAt.slice(0, 10)} · 댓글 {post.commentCount}
                </p>
              </Link>
            </li>
          ))}
        </ul>
      ) : null}

      {result ? (
        <Pagination page={result.page} totalPages={result.totalPages}
          onChange={(p) => updateParams({ page: p > 0 ? String(p) : '' })} />
      ) : null}
    </div>
  )
}
