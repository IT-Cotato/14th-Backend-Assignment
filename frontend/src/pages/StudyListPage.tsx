import { useEffect, useState } from 'react'
import type { ChangeEvent, FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { PARTS } from '../api/auth'
import type { Part } from '../api/auth'
import { getErrorMessage } from '../api/client'
import {
  STUDY_STATUSES,
  STUDY_STATUS_LABEL,
  formatDateTime,
  getStudyPosts,
} from '../api/studyPosts'
import type { PageResponse, StudyPostSummary, StudyStatus } from '../api/studyPosts'
import { ErrorMessage } from '../components/ErrorMessage'
import { FormField } from '../components/FormField'
import { StatusBadge } from '../components/StatusBadge'
import { cardClass, inputClass } from '../components/styles'

const PAGE_SIZE = 10

const pageButtonClass =
  'rounded-md border border-gray-300 bg-white px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50'

interface ListResult {
  key: string
  data: PageResponse<StudyPostSummary> | null
  error: string | null
}

// URL 값이 올바르지 않으면 기본값으로 본다 (서버 400 방지)
function readQuery(searchParams: URLSearchParams) {
  const keyword = searchParams.get('keyword') ?? ''
  const rawStatus = searchParams.get('status') ?? ''
  const rawPart = searchParams.get('part') ?? ''
  const rawPage = Number(searchParams.get('page'))

  const status = (STUDY_STATUSES as readonly string[]).includes(rawStatus)
    ? (rawStatus as StudyStatus)
    : ('' as const)
  const part = (PARTS as readonly string[]).includes(rawPart) ? (rawPart as Part) : ('' as const)
  // URL의 page는 1부터, API는 0부터
  const page = Number.isInteger(rawPage) && rawPage >= 1 ? rawPage : 1

  return { keyword, status, part, page }
}

export function StudyListPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const { keyword, status, part, page } = readQuery(searchParams)
  const queryKey = JSON.stringify({ keyword, status, part, page })

  const [result, setResult] = useState<ListResult | null>(null)
  const loading = result?.key !== queryKey

  useEffect(() => {
    let ignore = false
    getStudyPosts({ keyword, status, part, page: page - 1, size: PAGE_SIZE })
      .then((data) => {
        if (!ignore) setResult({ key: queryKey, data, error: null })
      })
      .catch((err) => {
        if (!ignore) setResult({ key: queryKey, data: null, error: getErrorMessage(err) })
      })
    return () => {
      ignore = true
    }
  }, [queryKey, keyword, status, part, page])

  // 값이 비어 있으면 URL에서 뺀다. 검색/필터를 바꾸면 1페이지로 돌아간다
  const updateQuery = (changes: Partial<Record<'keyword' | 'status' | 'part' | 'page', string>>) => {
    const next = new URLSearchParams(searchParams)
    for (const [name, value] of Object.entries(changes)) {
      if (value) next.set(name, value)
      else next.delete(name)
    }
    if (!('page' in changes)) next.delete('page')
    setSearchParams(next)
  }

  const handleSearch = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const value = new FormData(e.currentTarget).get('keyword')
    updateQuery({ keyword: typeof value === 'string' ? value.trim() : '' })
  }

  const handleFilterChange = (e: ChangeEvent<HTMLSelectElement>) => {
    updateQuery({ [e.target.name]: e.target.value })
  }

  const goToPage = (target: number) => {
    updateQuery({ page: target === 1 ? '' : String(target) })
  }

  const data = result?.key === queryKey ? result.data : null
  const error = result?.key === queryKey ? result.error : null
  const totalPages = data?.totalPages ?? 0

  return (
    <section className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-2xl font-bold text-gray-900">스터디 모집글</h1>
        <Link
          to="/studies/new"
          className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700"
        >
          글쓰기
        </Link>
      </div>

      <div className={`${cardClass} flex flex-col gap-4`}>
        {/* key로 URL 키워드가 바뀌면(뒤로 가기 등) 입력값도 다시 맞춘다 */}
        <form key={keyword} role="search" onSubmit={handleSearch} className="flex flex-col gap-2 sm:flex-row sm:items-end">
          <div className="flex-1">
            <FormField id="study-keyword" label="키워드">
              <input id="study-keyword" name="keyword" type="search" defaultValue={keyword}
                placeholder="제목 또는 내용" className={inputClass} />
            </FormField>
          </div>
          <button type="submit"
            className="rounded-md bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700">
            검색
          </button>
        </form>
        <div className="grid grid-cols-2 gap-2">
          <FormField id="study-status" label="모집 상태">
            <select id="study-status" name="status" value={status} onChange={handleFilterChange}
              className={inputClass}>
              <option value="">전체</option>
              {STUDY_STATUSES.map((value) => (
                <option key={value} value={value}>
                  {STUDY_STATUS_LABEL[value]}
                </option>
              ))}
            </select>
          </FormField>
          <FormField id="study-part" label="작성자 파트">
            <select id="study-part" name="part" value={part} onChange={handleFilterChange}
              className={inputClass}>
              <option value="">전체</option>
              {PARTS.map((value) => (
                <option key={value} value={value}>
                  {value}
                </option>
              ))}
            </select>
          </FormField>
        </div>
      </div>

      {loading && <p className="py-6 text-center text-gray-500">불러오는 중...</p>}
      <ErrorMessage message={error} />

      {data && data.content.length === 0 && (
        <p className="rounded-lg border border-dashed border-gray-300 bg-white py-10 text-center text-gray-500">
          모집글이 없습니다.
        </p>
      )}

      {data && data.content.length > 0 && (
        <ul className="flex flex-col gap-2">
          {data.content.map((post) => (
            <li key={post.postId}>
              <Link
                to={`/studies/${post.postId}`}
                className="block rounded-lg border border-gray-200 bg-white p-4 shadow-sm hover:border-indigo-300 hover:bg-indigo-50/30"
              >
                <div className="flex items-start gap-2">
                  <StatusBadge status={post.studyStatus} />
                  <h2 className="min-w-0 flex-1 break-words font-semibold text-gray-900">
                    {post.title}
                  </h2>
                </div>
                <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-gray-500">
                  <span>
                    {post.author.nickname} · {post.author.generation}기 · {post.author.part}
                  </span>
                  <span>댓글 {post.commentCount}</span>
                  <span>{formatDateTime(post.createdAt)}</span>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}

      {data && totalPages > 1 && (
        <nav aria-label="페이지 이동" className="flex items-center justify-center gap-3">
          <button type="button" onClick={() => goToPage(page - 1)} disabled={page <= 1}
            className={pageButtonClass}>
            이전
          </button>
          <span className="text-sm text-gray-700">
            {page} / {totalPages}
          </span>
          <button type="button" onClick={() => goToPage(page + 1)} disabled={page >= totalPages}
            className={pageButtonClass}>
            다음
          </button>
        </nav>
      )}
    </section>
  )
}
