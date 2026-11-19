import { useEffect, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getErrorMessage } from '../api/client'
import { getStudyPosts } from '../api/studyPosts'
import {
  PART_LABELS,
  PARTS,
  STATUS_LABELS,
  STUDY_STATUSES,
  type PageResponse,
  type Part,
  type StudyPostSummary,
  type StudyStatus,
} from '../api/types'
import { useAuth } from '../auth/AuthContext'
import StatusBadge from '../components/StatusBadge'
import { AuthorBadge, ErrorMessage, formatDate, inputClass, Loading, primaryButton, secondaryButton } from '../components/ui'

const PAGE_SIZE = 10

export default function StudyListPage() {
  const { isLoggedIn } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const keyword = searchParams.get('keyword') ?? ''
  const status = (searchParams.get('status') ?? '') as StudyStatus | ''
  const part = (searchParams.get('part') ?? '') as Part | ''
  const page = Number(searchParams.get('page') ?? 0)

  const [keywordInput, setKeywordInput] = useState(keyword)
  const [statusInput, setStatusInput] = useState(status)
  const [partInput, setPartInput] = useState(part)
  const [data, setData] = useState<PageResponse<StudyPostSummary> | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let ignore = false
    setLoading(true)
    setError(null)
    getStudyPosts({ keyword, status, part, page, size: PAGE_SIZE })
      .then((res) => !ignore && setData(res))
      .catch((err) => !ignore && setError(getErrorMessage(err, '모집글을 불러오지 못했어요.')))
      .finally(() => !ignore && setLoading(false))
    return () => {
      ignore = true
    }
  }, [keyword, status, part, page])

  const applyFilters = (next: { keyword?: string; status?: string; part?: string; page?: number }) => {
    const merged = { keyword, status, part, page: 0, ...next }
    const params = new URLSearchParams()
    if (merged.keyword) params.set('keyword', merged.keyword)
    if (merged.status) params.set('status', merged.status)
    if (merged.part) params.set('part', merged.part)
    if (merged.page) params.set('page', String(merged.page))
    setSearchParams(params)
  }

  const handleSearch = (e: FormEvent) => {
    e.preventDefault()
    applyFilters({ keyword: keywordInput.trim(), status: statusInput, part: partInput })
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-xl font-bold">스터디 모집글</h1>
        {isLoggedIn && (
          <Link to="/studies/new" className={primaryButton}>
            모집글 작성
          </Link>
        )}
      </div>

      <form onSubmit={handleSearch} className="grid grid-cols-1 gap-3 rounded-lg border border-gray-200 bg-white p-4 sm:grid-cols-[1fr_auto_auto_auto] sm:items-end">
        <div className="space-y-1">
          <label htmlFor="keyword" className="block text-sm font-medium text-gray-700">검색어</label>
          <input id="keyword" className={inputClass} placeholder="제목 또는 내용" value={keywordInput} onChange={(e) => setKeywordInput(e.target.value)} />
        </div>
        <div className="space-y-1">
          <label htmlFor="status" className="block text-sm font-medium text-gray-700">모집 상태</label>
          <select id="status" className={inputClass} value={statusInput} onChange={(e) => setStatusInput(e.target.value as StudyStatus | '')}>
            <option value="">전체</option>
            {STUDY_STATUSES.map((s) => (
              <option key={s} value={s}>{STATUS_LABELS[s]}</option>
            ))}
          </select>
        </div>
        <div className="space-y-1">
          <label htmlFor="part" className="block text-sm font-medium text-gray-700">파트</label>
          <select id="part" className={inputClass} value={partInput} onChange={(e) => setPartInput(e.target.value as Part | '')}>
            <option value="">전체</option>
            {PARTS.map((p) => (
              <option key={p} value={p}>{PART_LABELS[p]} ({p})</option>
            ))}
          </select>
        </div>
        <button type="submit" className={primaryButton}>검색</button>
      </form>

      <ErrorMessage message={error} />
      {loading ? (
        <Loading />
      ) : data && data.content.length === 0 ? (
        <p className="py-8 text-center text-sm text-gray-500">조건에 맞는 모집글이 없어요.</p>
      ) : (
        data && (
          <ul className="space-y-3">
            {data.content.map((post) => (
              <li key={post.postId} className="rounded-lg border border-gray-200 bg-white p-4 hover:border-indigo-300">
                <div className="flex items-start justify-between gap-3">
                  <Link to={`/studies/${post.postId}`} className="min-w-0 break-words font-semibold text-gray-900 hover:text-indigo-600">
                    {post.title}
                  </Link>
                  <StatusBadge status={post.studyStatus} />
                </div>
                <div className="mt-2 flex flex-wrap items-center justify-between gap-2 text-xs text-gray-500">
                  <AuthorBadge author={post.author} />
                  <span>
                    신청 {post.commentCount} · {formatDate(post.createdAt)}
                  </span>
                </div>
              </li>
            ))}
          </ul>
        )
      )}

      {data && data.totalPages > 1 && (
        <nav aria-label="페이지 이동" className="flex items-center justify-center gap-3">
          <button type="button" className={secondaryButton} disabled={page <= 0} onClick={() => applyFilters({ page: page - 1 })}>
            이전
          </button>
          <span className="text-sm text-gray-600">
            {page + 1} / {data.totalPages}
          </span>
          <button type="button" className={secondaryButton} disabled={page + 1 >= data.totalPages} onClick={() => applyFilters({ page: page + 1 })}>
            다음
          </button>
        </nav>
      )}
    </div>
  )
}
