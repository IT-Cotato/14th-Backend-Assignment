import { useEffect, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'
import { getErrorMessage } from '../api/errors'
import { getStudyPosts } from '../api/studyPosts'
import {
  PART_LABELS,
  PARTS,
  STUDY_STATUS_LABELS,
  STUDY_STATUSES,
  type PageResponse,
  type StudyPostSummary,
} from '../api/types'
import { ErrorAlert } from '../components/ErrorAlert'
import { LoadingStatus } from '../components/LoadingStatus'
import { Pagination } from '../components/Pagination'
import { StatusBadge } from '../components/StatusBadge'
import { inputClassName, smallPrimaryButtonClassName } from '../components/form'
import { formatAuthor, formatDateTime } from '../components/format'

const PAGE_SIZE = 10

// 검색 조건과 페이지는 주소의 쿼리(?keyword=&status=&part=&page=)에 둡니다.
// 새로 고치거나 뒤로 가도 같은 목록을 볼 수 있습니다.
export function StudiesPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const keyword = searchParams.get('keyword') ?? ''
  const status = searchParams.get('status') ?? ''
  const part = searchParams.get('part') ?? ''
  const page = Math.max(0, Number(searchParams.get('page') ?? '0') || 0)

  // 응답이 어떤 조건(requestKey)의 결과인지 함께 저장합니다.
  // 지금 조건과 다르면 불러오는 중으로 봅니다. (effect 안에서 로딩 상태를 따로 바꾸지 않습니다)
  const requestKey = JSON.stringify([keyword, status, part, page])
  const [loaded, setLoaded] = useState<{
    key: string
    result: PageResponse<StudyPostSummary> | null
    error: string | null
  } | null>(null)
  const isLoading = loaded?.key !== requestKey
  const result = isLoading ? null : loaded.result
  const error = isLoading ? null : loaded.error

  useEffect(() => {
    let cancelled = false
    getStudyPosts({ keyword, status, part, page, size: PAGE_SIZE })
      .then((data) => {
        if (!cancelled) setLoaded({ key: requestKey, result: data, error: null })
      })
      .catch((caught: unknown) => {
        if (!cancelled) setLoaded({ key: requestKey, result: null, error: getErrorMessage(caught) })
      })
    return () => {
      cancelled = true
    }
  }, [requestKey, keyword, status, part, page])

  // 검색하면 첫 페이지부터 다시 봅니다.
  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const formData = new FormData(event.currentTarget)
    const next = new URLSearchParams()
    for (const name of ['keyword', 'status', 'part']) {
      const value = String(formData.get(name) ?? '').trim()
      if (value) next.set(name, value)
    }
    setSearchParams(next)
  }

  const toPage = (target: number) => {
    const next = new URLSearchParams(searchParams)
    next.set('page', String(target))
    return `/studies?${next.toString()}`
  }

  return (
    <section className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold text-gray-900">스터디 목록</h1>
        <Link to="/studies/new" className={smallPrimaryButtonClassName}>
          모집글 쓰기
        </Link>
      </div>

      {/* 주소가 바뀌면(뒤로 가기 등) 입력값도 주소에 맞게 다시 채웁니다 */}
      <form
        key={searchParams.toString()}
        role="search"
        aria-label="모집글 검색"
        onSubmit={handleSubmit}
        className="grid grid-cols-1 gap-3 rounded-lg bg-white p-4 shadow-sm sm:grid-cols-[1fr_auto_auto_auto] sm:items-end"
      >
        <div className="flex flex-col gap-1">
          <label htmlFor="search-keyword" className="text-sm font-medium text-gray-800">
            검색어
          </label>
          <input
            id="search-keyword"
            name="keyword"
            type="search"
            defaultValue={keyword}
            placeholder="제목, 내용"
            className={inputClassName}
          />
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor="search-status" className="text-sm font-medium text-gray-800">
            모집 상태
          </label>
          <select id="search-status" name="status" defaultValue={status} className={inputClassName}>
            <option value="">전체</option>
            {STUDY_STATUSES.map((value) => (
              <option key={value} value={value}>
                {STUDY_STATUS_LABELS[value]}
              </option>
            ))}
          </select>
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor="search-part" className="text-sm font-medium text-gray-800">
            파트
          </label>
          <select id="search-part" name="part" defaultValue={part} className={inputClassName}>
            <option value="">전체</option>
            {PARTS.map((value) => (
              <option key={value} value={value}>
                {value} ({PART_LABELS[value]})
              </option>
            ))}
          </select>
        </div>
        <button type="submit" className={`${smallPrimaryButtonClassName} py-2.5`}>
          검색
        </button>
      </form>

      {isLoading && <LoadingStatus />}
      <ErrorAlert message={error} />

      {result && (
        <>
          <p className="text-sm text-gray-600">전체 {result.totalElements}개</p>
          {result.content.length === 0 ? (
            <p className="rounded-lg bg-white p-6 text-center text-gray-600 shadow-sm">조건에 맞는 모집글이 없어요.</p>
          ) : (
            <ul aria-label="모집글 목록" className="flex flex-col gap-3">
              {result.content.map((post) => (
                <li key={post.postId} className="rounded-lg bg-white p-4 shadow-sm">
                  <div className="flex items-start gap-2">
                    <StatusBadge status={post.studyStatus} />
                    <Link
                      to={`/studies/${post.postId}`}
                      className="min-w-0 break-words font-semibold text-gray-900 hover:text-blue-700 hover:underline"
                    >
                      {post.title}
                    </Link>
                  </div>
                  <p className="mt-2 flex flex-wrap gap-x-3 gap-y-1 text-sm text-gray-600">
                    <span>{formatAuthor(post.author)}</span>
                    <span>댓글 {post.commentCount}</span>
                    <span>{formatDateTime(post.createdAt)}</span>
                  </p>
                </li>
              ))}
            </ul>
          )}
          <Pagination page={result.page} totalPages={result.totalPages} toPage={toPage} label="모집글 페이지" />
        </>
      )}
    </section>
  )
}
