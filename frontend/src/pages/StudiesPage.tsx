import { useEffect, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getErrorMessage } from '../api/error'
import { getStudyPosts } from '../api/studyPosts'
import {
  PARTS,
  PART_LABELS,
  STUDY_STATUSES,
  STUDY_STATUS_LABELS,
  type PageResponse,
  type Part,
  type StudyPostSummary,
  type StudyStatus,
} from '../api/types'
import { useAuth } from '../auth/useAuth'
import { AuthorInfo } from '../components/AuthorInfo'
import { ErrorAlert } from '../components/ErrorAlert'
import { SelectField, TextField } from '../components/FormField'
import { Loading } from '../components/Loading'
import { Pagination } from '../components/Pagination'
import { StatusBadge } from '../components/StatusBadge'
import { formatDate } from '../utils/date'

const PAGE_SIZE = 10

interface ListFilters {
  keyword: string
  status: StudyStatus | ''
  part: Part | ''
  page: number // 화면 기준(1부터)
}

// URL이 기준이에요. 잘못된 값(?status=FOO, ?page=abc)은 무시하고 전체 / 1페이지로 봐요 (서버 400을 띄우지 않으려고)
function parseFilters(searchParams: URLSearchParams): ListFilters {
  const status = searchParams.get('status') ?? ''
  const part = searchParams.get('part') ?? ''
  const pageText = searchParams.get('page') ?? ''
  const page = /^\d+$/.test(pageText) && Number(pageText) >= 1 ? Number(pageText) : 1
  return {
    keyword: (searchParams.get('keyword') ?? '').trim(),
    status: (STUDY_STATUSES as readonly string[]).includes(status) ? (status as StudyStatus) : '',
    part: (PARTS as readonly string[]).includes(part) ? (part as Part) : '',
    page,
  }
}

interface ListResult {
  key: string
  data?: PageResponse<StudyPostSummary>
  error?: string
}

export function StudiesPage() {
  const { status: authStatus } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const filters = parseFilters(searchParams)
  const queryKey = JSON.stringify(filters)

  // 마지막으로 받은 결과가 현재 쿼리의 것이 아니면 로딩 중으로 봐요
  const [result, setResult] = useState<ListResult | null>(null)
  const loading = result?.key !== queryKey

  useEffect(() => {
    const current: ListFilters = JSON.parse(queryKey)
    let cancelled = false
    getStudyPosts({
      keyword: current.keyword || undefined,
      status: current.status || undefined,
      part: current.part || undefined,
      page: current.page - 1, // API는 0부터
      size: PAGE_SIZE,
    })
      .then((data) => {
        if (!cancelled) setResult({ key: queryKey, data })
      })
      .catch((error: unknown) => {
        if (!cancelled) setResult({ key: queryKey, error: getErrorMessage(error) })
      })
    // 필터를 빠르게 바꿨을 때 늦게 도착한 이전 응답이 화면을 덮어쓰지 않게 해요
    return () => {
      cancelled = true
    }
  }, [queryKey])

  // 검색어·필터가 바뀌면 1페이지로 돌아가요
  const updateFilters = (changes: Partial<Pick<ListFilters, 'keyword' | 'status' | 'part'>>) => {
    const next = new URLSearchParams(searchParams)
    for (const [name, value] of Object.entries(changes)) {
      if (value) next.set(name, value)
      else next.delete(name)
    }
    next.delete('page')
    setSearchParams(next)
  }

  return (
    <section>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold text-slate-900">스터디 목록</h1>
        {authStatus === 'authenticated' && (
          <Link
            to="/studies/new"
            className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-500"
          >
            모집글 작성
          </Link>
        )}
      </div>

      <SearchForm
        // 뒤로 가기 등으로 URL의 검색어가 바뀌면 입력창도 그 값으로 다시 그려요
        key={filters.keyword}
        filters={filters}
        onKeywordSubmit={(keyword) => updateFilters({ keyword })}
        onStatusChange={(status) => updateFilters({ status })}
        onPartChange={(part) => updateFilters({ part })}
      />

      <div className="mt-6">
        {loading ? (
          <Loading />
        ) : result?.error ? (
          <ErrorAlert message={result.error} />
        ) : result?.data && result.data.content.length === 0 ? (
          <p className="rounded-xl border border-dashed border-slate-300 bg-white py-12 text-center text-slate-500">
            조건에 맞는 모집글이 없어요.
          </p>
        ) : result?.data ? (
          <>
            <ul className="space-y-3">
              {result.data.content.map((post) => (
                <StudyPostItem key={post.postId} post={post} />
              ))}
            </ul>
            <Pagination page={filters.page} totalPages={result.data.totalPages} />
          </>
        ) : null}
      </div>
    </section>
  )
}

interface SearchFormProps {
  filters: ListFilters
  onKeywordSubmit: (keyword: string) => void
  onStatusChange: (status: StudyStatus | '') => void
  onPartChange: (part: Part | '') => void
}

function SearchForm({ filters, onKeywordSubmit, onStatusChange, onPartChange }: SearchFormProps) {
  const [keyword, setKeyword] = useState(filters.keyword)

  const handleSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    onKeywordSubmit(keyword.trim())
  }

  return (
    <form
      role="search"
      onSubmit={handleSubmit}
      className="flex flex-col gap-3 rounded-xl border border-slate-200 bg-white p-4 shadow-sm sm:flex-row sm:items-end"
    >
      <div className="flex flex-1 items-end gap-2">
        <div className="flex-1">
          <TextField
            label="검색어"
            name="keyword"
            type="search"
            placeholder="제목이나 내용으로 검색"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
          />
        </div>
        <button
          type="submit"
          className="shrink-0 rounded-md bg-slate-800 px-4 py-2 text-base font-medium text-white hover:bg-slate-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-500"
        >
          검색
        </button>
      </div>
      <div className="grid grid-cols-2 gap-3 sm:w-80">
        <SelectField
          label="모집 상태"
          name="status"
          value={filters.status}
          onChange={(e) => onStatusChange(e.target.value as StudyStatus | '')}
        >
          <option value="">전체</option>
          {STUDY_STATUSES.map((status) => (
            <option key={status} value={status}>
              {STUDY_STATUS_LABELS[status]}
            </option>
          ))}
        </SelectField>
        <SelectField
          label="파트"
          name="part"
          value={filters.part}
          onChange={(e) => onPartChange(e.target.value as Part | '')}
        >
          <option value="">전체</option>
          {PARTS.map((part) => (
            <option key={part} value={part}>
              {part} ({PART_LABELS[part]})
            </option>
          ))}
        </SelectField>
      </div>
    </form>
  )
}

function StudyPostItem({ post }: { post: StudyPostSummary }) {
  return (
    <li className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm transition hover:border-indigo-300">
      <div className="flex items-start gap-2">
        <StatusBadge status={post.studyStatus} />
        <Link
          to={`/studies/${post.postId}`}
          className="min-w-0 flex-1 break-words font-semibold text-slate-900 hover:text-indigo-600 hover:underline"
        >
          {post.title}
        </Link>
      </div>
      <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1">
        <AuthorInfo author={post.author} />
        <span className="text-sm text-slate-400">{formatDate(post.createdAt)}</span>
        <span className="text-sm text-slate-500">댓글 {post.commentCount}</span>
      </div>
    </li>
  )
}
