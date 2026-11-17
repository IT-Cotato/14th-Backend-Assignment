import { useEffect, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'
import { fetchStudies } from '../api/studies'
import {
  PARTS,
  STUDY_STATUSES,
  type PageResponse,
  type Part,
  type StudyPostSummary,
  type StudyStatus,
} from '../api/types'
import { useAuth } from '../auth/useAuth'
import { SelectField, TextField } from '../components/FormFields'
import { StatusBadge } from '../components/StatusBadge'
import { cardClass, primaryButtonClass } from '../components/styles'
import { formatAuthor, formatDateTime } from '../utils/format'
import { getErrorMessage } from '../utils/messages'

const PAGE_SIZE = 10

interface Filters {
  keyword: string
  status: StudyStatus | ''
  part: Part | ''
}

// URL의 쿼리 문자열은 사용자가 직접 고칠 수 있으므로 허용된 값만 받아들인다.
function toStatus(value: string | null): StudyStatus | '' {
  return STUDY_STATUSES.find((s) => s.value === value)?.value ?? ''
}

function toPart(value: string | null): Part | '' {
  return PARTS.find((p) => p.value === value)?.value ?? ''
}

function toPage(value: string | null): number {
  const page = Number(value)
  return Number.isInteger(page) && page >= 0 ? page : 0
}

interface SearchFormProps {
  initial: Filters
  onSearch: (filters: Filters) => void
}

// 입력 중인 값은 이 컴포넌트가 따로 가진다. 검색 버튼을 눌러야 실제 검색 조건(URL)에 반영된다.
// 부모가 key를 바꿔서 다시 만들면(뒤로 가기 등으로 URL이 바뀐 경우) 입력창도 URL 값으로 초기화된다.
function SearchForm({ initial, onSearch }: SearchFormProps) {
  const [draft, setDraft] = useState<Filters>(initial)

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    onSearch(draft)
  }

  return (
    <form onSubmit={handleSubmit} className={`${cardClass} mb-6 space-y-4`}>
      <TextField
        label="검색어"
        type="search"
        name="keyword"
        placeholder="제목이나 내용으로 검색"
        value={draft.keyword}
        onChange={(event) => setDraft((prev) => ({ ...prev, keyword: event.target.value }))}
      />
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <SelectField
          label="모집 상태"
          name="status"
          value={draft.status}
          onChange={(event) => setDraft((prev) => ({ ...prev, status: toStatus(event.target.value) }))}
        >
          <option value="">전체</option>
          {STUDY_STATUSES.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </SelectField>
        <SelectField
          label="작성자 파트"
          name="part"
          value={draft.part}
          onChange={(event) => setDraft((prev) => ({ ...prev, part: toPart(event.target.value) }))}
        >
          <option value="">전체</option>
          {PARTS.map((p) => (
            <option key={p.value} value={p.value}>
              {p.label}
            </option>
          ))}
        </SelectField>
      </div>
      <button type="submit" className={primaryButtonClass}>
        검색
      </button>
    </form>
  )
}

interface LoadedList {
  key: string
  data: PageResponse<StudyPostSummary> | null
  error: string | null
}

export function StudiesPage() {
  const { isAuthenticated } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()

  // 검색 조건은 URL에 담는다. 새로고침이나 뒤로 가기를 해도 검색 상태가 유지된다.
  const filters: Filters = {
    keyword: searchParams.get('keyword') ?? '',
    status: toStatus(searchParams.get('status')),
    part: toPart(searchParams.get('part')),
  }
  const page = toPage(searchParams.get('page'))
  const { keyword, status, part } = filters

  // 마지막으로 도착한 결과와 그 결과를 요청할 때의 조건(key)을 함께 저장한다.
  // 현재 조건과 key가 다르면 아직 로딩 중이고, 늦게 도착한 옛 응답은 key가 달라 무시된다.
  const requestKey = JSON.stringify([keyword, status, part, page])
  const [loaded, setLoaded] = useState<LoadedList | null>(null)

  useEffect(() => {
    let cancelled = false
    fetchStudies({ keyword, status, part, page, size: PAGE_SIZE })
      .then((data) => {
        if (!cancelled) setLoaded({ key: requestKey, data, error: null })
      })
      .catch((err) => {
        if (!cancelled) setLoaded({ key: requestKey, data: null, error: getErrorMessage(err) })
      })
    return () => {
      cancelled = true
    }
  }, [keyword, status, part, page, requestKey])

  const current = loaded?.key === requestKey ? loaded : null
  const loading = current === null
  const error = current?.error ?? null
  // 로딩 중에는 이전 결과를 그대로 보여줘서 화면이 깜빡이지 않게 한다
  const posts = (current ? current.data : loaded?.data)?.content ?? []
  const totalPages = (current ? current.data : loaded?.data)?.totalPages ?? 0

  function updateQuery(next: Filters & { page: number }) {
    const params = new URLSearchParams()
    if (next.keyword.trim()) params.set('keyword', next.keyword.trim())
    if (next.status) params.set('status', next.status)
    if (next.part) params.set('part', next.part)
    if (next.page > 0) params.set('page', String(next.page))
    setSearchParams(params)
  }

  return (
    <div>
      <div className="mb-6 flex items-center justify-between gap-3">
        <h1 className="text-2xl font-bold">스터디 목록</h1>
        {isAuthenticated && (
          <Link
            to="/studies/new"
            className="rounded-md bg-indigo-600 px-3 py-2 text-sm font-semibold text-white hover:bg-indigo-500"
          >
            모집글 작성
          </Link>
        )}
      </div>

      {/* 조건이 바뀌면 첫 페이지부터 보여준다 */}
      <SearchForm
        key={JSON.stringify(filters)}
        initial={filters}
        onSearch={(next) => updateQuery({ ...next, page: 0 })}
      />

      {error && (
        <p role="alert" className="mb-4 text-sm text-red-600">
          {error}
        </p>
      )}

      {loading && <p className="py-6 text-center text-slate-500">불러오는 중이에요...</p>}

      {!loading && !error && posts.length === 0 && (
        <p className="py-6 text-center text-slate-500">조건에 맞는 모집글이 없어요.</p>
      )}

      <ul className="space-y-3">
        {posts.map((post) => (
          <li key={post.postId}>
            <Link to={`/studies/${post.postId}`} className={`${cardClass} block hover:border-indigo-300`}>
              <div className="mb-1 flex items-center gap-2">
                <StatusBadge status={post.studyStatus} />
                <span className="font-semibold text-slate-900">{post.title}</span>
              </div>
              <p className="text-sm text-slate-500">
                {formatAuthor(post.author)} · {formatDateTime(post.createdAt)} · 댓글 {post.commentCount}
              </p>
            </Link>
          </li>
        ))}
      </ul>

      {totalPages > 1 && (
        <nav aria-label="페이지 이동" className="mt-6 flex items-center justify-center gap-4 text-sm">
          <button
            type="button"
            disabled={page <= 0}
            onClick={() => updateQuery({ ...filters, page: page - 1 })}
            className="rounded-md border border-slate-300 bg-white px-3 py-1.5 disabled:cursor-not-allowed disabled:opacity-40"
          >
            이전
          </button>
          <span className="text-slate-600">
            {page + 1} / {totalPages}
          </span>
          <button
            type="button"
            disabled={page + 1 >= totalPages}
            onClick={() => updateQuery({ ...filters, page: page + 1 })}
            className="rounded-md border border-slate-300 bg-white px-3 py-1.5 disabled:cursor-not-allowed disabled:opacity-40"
          >
            다음
          </button>
        </nav>
      )}
    </div>
  )
}
