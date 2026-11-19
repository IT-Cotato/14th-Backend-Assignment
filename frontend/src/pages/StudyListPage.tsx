import { useState, type FormEvent } from 'react'
import { Link, useLocation, useSearchParams } from 'react-router'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { getErrorMessage } from '../api/errors'
import { searchStudyPosts, studyPostKeys } from '../api/studyPosts'
import {
  PART_LABEL,
  PARTS,
  STUDY_STATUS_LABEL,
  type Part,
  type StudyPostSearchParams,
  type StudyStatus,
} from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { Pagination } from '../components/Pagination'
import { StatusBadge } from '../components/StatusBadge'
import { AuthorInfo, ErrorMessage, InfoMessage, inputClass, Loading, primaryButtonClass } from '../components/ui'
import { formatDateTime } from '../utils'

const PAGE_SIZE = 10
const STATUSES: StudyStatus[] = ['RECRUITING', 'CLOSED']

function readParams(searchParams: URLSearchParams): StudyPostSearchParams {
  const status = searchParams.get('status')
  const part = searchParams.get('part')
  const page = Number(searchParams.get('page') ?? 0)
  return {
    keyword: searchParams.get('keyword') || undefined,
    status: STATUSES.includes(status as StudyStatus) ? (status as StudyStatus) : undefined,
    part: PARTS.includes(part as Part) ? (part as Part) : undefined,
    page: Number.isInteger(page) && page > 0 ? page : 0,
    size: PAGE_SIZE,
  }
}

export function StudyListPage() {
  const { isLoggedIn } = useAuth()
  const location = useLocation()
  const flashMessage = (location.state as { message?: string } | null)?.message
  const [searchParams, setSearchParams] = useSearchParams()
  const params = readParams(searchParams)

  const [keyword, setKeyword] = useState(params.keyword ?? '')
  const [status, setStatus] = useState<string>(params.status ?? '')
  const [part, setPart] = useState<string>(params.part ?? '')

  const { data, isPending, isError, error, isFetching } = useQuery({
    queryKey: studyPostKeys.list(params),
    queryFn: () => searchStudyPosts(params),
    placeholderData: keepPreviousData,
  })

  const applySearch = (next: { keyword?: string; status?: string; part?: string; page?: number }) => {
    const query = new URLSearchParams()
    if (next.keyword?.trim()) query.set('keyword', next.keyword.trim())
    if (next.status) query.set('status', next.status)
    if (next.part) query.set('part', next.part)
    if (next.page) query.set('page', String(next.page))
    setSearchParams(query)
  }

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    applySearch({ keyword, status, part, page: 0 })
  }

  return (
    <section>
      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold">스터디 모집글</h1>
        {isLoggedIn && (
          <Link to="/studies/new" className={primaryButtonClass}>
            모집글 작성
          </Link>
        )}
      </div>

      {flashMessage && (
        <div className="mb-4">
          <InfoMessage>{flashMessage}</InfoMessage>
        </div>
      )}

      <form
        role="search"
        onSubmit={handleSubmit}
        className="mb-5 grid grid-cols-1 gap-3 rounded-xl bg-white p-4 shadow-sm sm:grid-cols-[1fr_auto_auto_auto] sm:items-end"
      >
        <div className="space-y-1">
          <label htmlFor="keyword" className="block text-xs font-medium text-stone-600">
            검색어
          </label>
          <input
            id="keyword"
            type="search"
            placeholder="제목이나 내용으로 검색"
            className={inputClass}
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
          />
        </div>
        <div className="space-y-1">
          <label htmlFor="status" className="block text-xs font-medium text-stone-600">
            모집 상태
          </label>
          <select id="status" className={inputClass} value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">전체</option>
            {STATUSES.map((s) => (
              <option key={s} value={s}>
                {STUDY_STATUS_LABEL[s]}
              </option>
            ))}
          </select>
        </div>
        <div className="space-y-1">
          <label htmlFor="part" className="block text-xs font-medium text-stone-600">
            작성자 파트
          </label>
          <select id="part" className={inputClass} value={part} onChange={(e) => setPart(e.target.value)}>
            <option value="">전체</option>
            {PARTS.map((p) => (
              <option key={p} value={p}>
                {PART_LABEL[p]} ({p})
              </option>
            ))}
          </select>
        </div>
        <button type="submit" className={primaryButtonClass}>
          검색
        </button>
      </form>

      {isPending ? (
        <Loading />
      ) : isError ? (
        <ErrorMessage>{getErrorMessage(error)}</ErrorMessage>
      ) : (
        <>
          <p className="mb-2 text-sm text-stone-500" aria-live="polite">
            총 {data.totalElements}개{isFetching && ' · 불러오는 중...'}
          </p>
          {data.content.length === 0 ? (
            <p className="rounded-xl bg-white py-12 text-center text-stone-500 shadow-sm">조건에 맞는 모집글이 없어요.</p>
          ) : (
            <ul aria-label="모집글 목록" className="divide-y divide-stone-100 rounded-xl bg-white shadow-sm">
              {data.content.map((post) => (
                <li key={post.postId} className="px-4 py-4 sm:px-5">
                  <div className="flex items-start gap-2">
                    <StatusBadge status={post.studyStatus} />
                    <Link
                      to={`/studies/${post.postId}`}
                      className="min-w-0 break-words font-semibold text-stone-900 hover:text-amber-700"
                    >
                      {post.title}
                    </Link>
                  </div>
                  <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1">
                    <AuthorInfo author={post.author} />
                    <span className="text-xs text-stone-500">신청 댓글 {post.commentCount}</span>
                    <span className="text-xs text-stone-400">{formatDateTime(post.createdAt)}</span>
                  </div>
                </li>
              ))}
            </ul>
          )}
          <Pagination
            page={data.page}
            totalPages={data.totalPages}
            onChange={(page) => applySearch({ ...params, page })}
            label="모집글 페이지"
          />
        </>
      )}
    </section>
  )
}
