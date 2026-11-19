import { useEffect, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'

import { getErrorMessage } from '../api/error'
import { getStudyPosts } from '../api/studyPosts'
import {
  isPart,
  isStudyStatus,
  PART_OPTIONS,
  partLabel,
  STATUS_OPTIONS,
  statusLabel,
  type PageResponse,
  type StudyPostSummary,
} from '../api/types'
import { Pagination } from '../components/Pagination'
import { ErrorMessage, Loading } from '../components/StatusMessage'
import { formatDateTime } from '../utils/format'

const PAGE_SIZE = 10

const fieldClass =
  'mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-gray-900 focus:outline-none'

export function StudyListPage() {
  // 검색 조건은 URL 쿼리에 담아요. 새로고침하거나 뒤로 가도 조건이 유지돼요.
  const [searchParams, setSearchParams] = useSearchParams()
  const keyword = searchParams.get('keyword') ?? ''
  const statusParam = searchParams.get('status')
  const partParam = searchParams.get('part')
  // 목록에 없는 값이 주소에 들어오면 무시해요
  const status = isStudyStatus(statusParam) ? statusParam : undefined
  const part = isPart(partParam) ? partParam : undefined
  const page = Math.max(0, Number(searchParams.get('page')) || 0)

  // 어떤 조건에 대한 응답인지 key로 함께 저장해요.
  // 지금 조건의 key와 다르면 아직 불러오는 중이에요. (로딩 상태를 따로 두지 않아요)
  const requestKey = JSON.stringify({ keyword, status, part, page })
  const [loaded, setLoaded] = useState<{
    key: string
    result?: PageResponse<StudyPostSummary>
    error?: string
  } | null>(null)

  useEffect(() => {
    let cancelled = false
    const key = JSON.stringify({ keyword, status, part, page })
    getStudyPosts({ keyword: keyword || undefined, status, part, page, size: PAGE_SIZE })
      .then((data) => {
        if (!cancelled) setLoaded({ key, result: data })
      })
      .catch((err: unknown) => {
        if (!cancelled) setLoaded({ key, error: getErrorMessage(err) })
      })
    return () => {
      cancelled = true
    }
  }, [keyword, status, part, page])

  const isLoading = loaded?.key !== requestKey
  const result = isLoading ? null : (loaded?.result ?? null)
  const error = isLoading ? null : (loaded?.error ?? null)

  // 검색하면 첫 페이지부터 다시 보여줘요
  const handleSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const formData = new FormData(event.currentTarget)
    const next = new URLSearchParams()
    const nextKeyword = String(formData.get('keyword') ?? '').trim()
    const nextStatus = String(formData.get('status') ?? '')
    const nextPart = String(formData.get('part') ?? '')
    if (nextKeyword) next.set('keyword', nextKeyword)
    if (nextStatus) next.set('status', nextStatus)
    if (nextPart) next.set('part', nextPart)
    setSearchParams(next)
  }

  const handlePageChange = (nextPage: number) => {
    const next = new URLSearchParams(searchParams)
    next.set('page', String(nextPage))
    setSearchParams(next)
  }

  return (
    <section>
      <div className="mb-4 flex items-center justify-between gap-2">
        <h1 className="text-2xl font-bold">스터디 모집글</h1>
        <Link
          to="/studies/new"
          className="rounded-md bg-gray-900 px-3 py-2 text-sm font-medium text-white hover:bg-gray-700"
        >
          모집글 작성
        </Link>
      </div>

      {/* key: 주소의 조건이 바뀌면(뒤로 가기 등) 입력창도 그 값으로 다시 채워요 */}
      <form
        key={searchParams.toString()}
        onSubmit={handleSearch}
        role="search"
        className="mb-6 grid grid-cols-1 gap-3 rounded-lg border border-gray-200 bg-white p-4 sm:grid-cols-4 sm:items-end"
      >
        <div className="sm:col-span-2">
          <label htmlFor="keyword" className="text-sm font-medium">
            검색어
          </label>
          <input
            id="keyword"
            name="keyword"
            type="search"
            defaultValue={keyword}
            placeholder="제목 또는 내용"
            className={fieldClass}
          />
        </div>
        <div>
          <label htmlFor="status" className="text-sm font-medium">
            모집 상태
          </label>
          <select id="status" name="status" defaultValue={status ?? ''} className={fieldClass}>
            <option value="">전체</option>
            {STATUS_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label htmlFor="part" className="text-sm font-medium">
            파트
          </label>
          <select id="part" name="part" defaultValue={part ?? ''} className={fieldClass}>
            <option value="">전체</option>
            {PART_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>
        <button
          type="submit"
          className="rounded-md bg-gray-900 py-2 text-sm font-medium text-white hover:bg-gray-700 sm:col-span-4"
        >
          검색
        </button>
      </form>

      {error && <ErrorMessage message={error} />}
      {isLoading && <Loading />}

      {!isLoading && !error && result && (
        <>
          <p className="mb-2 text-sm text-gray-500">총 {result.totalElements}개</p>

          {result.content.length === 0 ? (
            <p className="py-8 text-center text-sm text-gray-500">조건에 맞는 모집글이 없어요.</p>
          ) : (
            <ul className="space-y-3">
              {result.content.map((post) => (
                <li key={post.postId} className="rounded-lg border border-gray-200 bg-white p-4">
                  <div className="mb-1 flex items-center gap-2">
                    <span
                      className={
                        post.studyStatus === 'RECRUITING'
                          ? 'rounded bg-green-100 px-2 py-0.5 text-xs font-medium text-green-800'
                          : 'rounded bg-gray-200 px-2 py-0.5 text-xs font-medium text-gray-700'
                      }
                    >
                      {statusLabel(post.studyStatus)}
                    </span>
                    <Link
                      to={`/studies/${post.postId}`}
                      className="min-w-0 truncate font-semibold text-gray-900 hover:underline"
                    >
                      {post.title}
                    </Link>
                  </div>
                  <p className="flex flex-wrap gap-x-3 gap-y-1 text-xs text-gray-500">
                    <span>
                      {post.author.nickname} · {post.author.generation}기 · {partLabel(post.author.part)}
                    </span>
                    <span>댓글 {post.commentCount}</span>
                    <span>{formatDateTime(post.createdAt)}</span>
                  </p>
                </li>
              ))}
            </ul>
          )}

          <Pagination page={result.page} totalPages={result.totalPages} onChange={handlePageChange} />
        </>
      )}
    </section>
  )
}
