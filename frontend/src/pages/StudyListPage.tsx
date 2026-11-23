import { useQuery } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'
import { getErrorMessage } from '../api/client'
import { fetchStudyPosts } from '../api/studyPosts'
import { ErrorMessage } from '../components/ErrorMessage'
import { Loading } from '../components/Loading'
import { Pagination } from '../components/Pagination'
import { StatusBadge } from '../components/StatusBadge'
import {
  cardClass,
  inputClass,
  labelClass,
  primaryButtonClass,
  secondaryButtonClass,
} from '../components/styles'
import { formatAuthor, formatDateTime } from '../lib/format'
import {
  PART_LABELS,
  PARTS,
  STUDY_STATUS_LABELS,
  STUDY_STATUSES,
  type Part,
  type StudyStatus,
} from '../types'

const PAGE_SIZE = 10

function toStatus(value: string | null): StudyStatus | undefined {
  return STUDY_STATUSES.find((status) => status === value)
}

function toPart(value: string | null): Part | undefined {
  return PARTS.find((part) => part === value)
}

function toPage(value: string | null): number {
  const page = Number(value)
  return Number.isInteger(page) && page > 0 ? page : 0
}

export function StudyListPage() {
  // 검색 조건은 주소의 쿼리스트링에 둔다. 새로고침하거나 뒤로 가도 조건이 유지된다.
  const [searchParams, setSearchParams] = useSearchParams()
  const keyword = searchParams.get('keyword') ?? ''
  const status = toStatus(searchParams.get('status'))
  const part = toPart(searchParams.get('part'))
  const page = toPage(searchParams.get('page'))

  const { data, isPending, isError, error } = useQuery({
    queryKey: ['studyPosts', { keyword, status, part, page }],
    queryFn: () =>
      fetchStudyPosts({ keyword: keyword || undefined, status, part, page, size: PAGE_SIZE }),
  })

  const buildHref = (nextPage: number) => {
    const params = new URLSearchParams(searchParams)
    params.set('page', String(nextPage))
    return `/studies?${params.toString()}`
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-xl font-bold">스터디 모집글</h1>
        <Link to="/studies/new" className={primaryButtonClass}>
          모집글 작성
        </Link>
      </div>

      <SearchForm
        values={{ keyword, status: status ?? '', part: part ?? '' }}
        onSearch={(next) => {
          const params = new URLSearchParams()
          if (next.keyword) params.set('keyword', next.keyword)
          if (next.status) params.set('status', next.status)
          if (next.part) params.set('part', next.part)
          setSearchParams(params)
        }}
      />

      {isPending && <Loading />}
      {isError && <ErrorMessage message={getErrorMessage(error)} />}
      {data && (
        <>
          <p className="text-sm text-slate-500">총 {data.totalElements}개</p>
          {data.content.length === 0 ? (
            <p className={`text-center text-sm text-slate-500 ${cardClass}`}>
              조건에 맞는 모집글이 없습니다.
            </p>
          ) : (
            <ul className="space-y-3">
              {data.content.map((post) => (
                <li key={post.postId} className={cardClass}>
                  <div className="flex items-start justify-between gap-3">
                    <Link
                      to={`/studies/${post.postId}`}
                      className="break-all text-base font-semibold text-slate-900 hover:text-indigo-600 hover:underline"
                    >
                      {post.title}
                    </Link>
                    <StatusBadge status={post.studyStatus} />
                  </div>
                  <p className="mt-2 flex flex-wrap gap-x-3 gap-y-1 text-xs text-slate-500">
                    <span>{formatAuthor(post.author)}</span>
                    <span>댓글 {post.commentCount}개</span>
                    <span>{formatDateTime(post.createdAt)}</span>
                  </p>
                </li>
              ))}
            </ul>
          )}
          <Pagination page={data.page} totalPages={data.totalPages} buildHref={buildHref} />
        </>
      )}
    </div>
  )
}

interface SearchValues {
  keyword: string
  status: StudyStatus | ''
  part: Part | ''
}

interface SearchFormProps {
  values: SearchValues
  onSearch: (values: SearchValues) => void
}

function toSearchKey({ keyword, status, part }: SearchValues): string {
  return JSON.stringify([keyword, status, part])
}

function SearchForm({ values, onSearch }: SearchFormProps) {
  const [keyword, setKeyword] = useState(values.keyword)
  const [status, setStatus] = useState<StudyStatus | ''>(values.status)
  const [part, setPart] = useState<Part | ''>(values.part)

  // 뒤로 가기처럼 폼 밖에서 주소의 검색 조건이 바뀌면 입력값을 주소에 맞춘다.
  // 방금 이 폼이 제출한 조건이 주소에 반영된 것이면 그대로 둔다.
  // 주소 반영은 transition 으로 늦게 커밋되므로, 그사이 입력한 값을 지우지 않기 위해서다
  const urlKey = toSearchKey(values)
  const [prevUrlKey, setPrevUrlKey] = useState(urlKey)
  const [submittedKey, setSubmittedKey] = useState<string | null>(null)
  if (urlKey !== prevUrlKey) {
    setPrevUrlKey(urlKey)
    setSubmittedKey(null)
    if (urlKey !== submittedKey) {
      setKeyword(values.keyword)
      setStatus(values.status)
      setPart(values.part)
    }
  }

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    const next = { keyword: keyword.trim(), status, part }
    setSubmittedKey(toSearchKey(next))
    onSearch(next)
  }

  return (
    <form
      onSubmit={handleSubmit}
      role="search"
      className={`grid grid-cols-1 gap-3 sm:grid-cols-[1fr_auto_auto_auto] sm:items-end ${cardClass}`}
    >
      <div>
        <label htmlFor="search-keyword" className={labelClass}>
          검색어
        </label>
        <input
          id="search-keyword"
          type="search"
          placeholder="제목이나 내용으로 검색"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          className={inputClass}
        />
      </div>
      <div>
        <label htmlFor="search-status" className={labelClass}>
          모집 상태
        </label>
        <select
          id="search-status"
          value={status}
          onChange={(event) => setStatus(toStatus(event.target.value) ?? '')}
          className={inputClass}
        >
          <option value="">전체</option>
          {STUDY_STATUSES.map((value) => (
            <option key={value} value={value}>
              {STUDY_STATUS_LABELS[value]}
            </option>
          ))}
        </select>
      </div>
      <div>
        <label htmlFor="search-part" className={labelClass}>
          파트
        </label>
        <select
          id="search-part"
          value={part}
          onChange={(event) => setPart(toPart(event.target.value) ?? '')}
          className={inputClass}
        >
          <option value="">전체</option>
          {PARTS.map((value) => (
            <option key={value} value={value}>
              {value} ({PART_LABELS[value]})
            </option>
          ))}
        </select>
      </div>
      <button type="submit" className={secondaryButtonClass}>
        검색
      </button>
    </form>
  )
}
