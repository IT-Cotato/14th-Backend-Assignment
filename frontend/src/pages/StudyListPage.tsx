import { useEffect, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'
import { getStudyPosts } from '../api/studyPosts'
import { toApiError, type ApiError } from '../api/errors'
import AuthorInfo from '../components/AuthorInfo'
import LoadingState from '../components/LoadingState'
import MessageState from '../components/MessageState'
import Pagination from '../components/Pagination'
import StatusBadge from '../components/StatusBadge'
import { inputClass, primaryButtonClass, secondaryButtonClass } from '../components/FormField'
import { PARTS, PART_LABELS, type PageResponse, type Part } from '../types/api'
import {
    STUDY_STATUSES,
    STUDY_STATUS_LABELS,
    type StudyPostSummary,
    type StudyStatus,
} from '../types/studyPost'
import { formatDate } from '../utils/format'

const PAGE_SIZE = 10

function parseStatus(value: string | null): StudyStatus | undefined {
    return STUDY_STATUSES.find((s) => s === value)
}

function parsePart(value: string | null): Part | undefined {
    return PARTS.find((p) => p === value)
}

// URL의 page는 1부터, API의 page는 0부터 시작해요.
function parsePage(value: string | null): number {
    const page = Number(value)
    return Number.isInteger(page) && page >= 1 ? page - 1 : 0
}

type ListState =
    | { kind: 'loading' }
    | { kind: 'error'; error: ApiError }
    | { kind: 'success'; data: PageResponse<StudyPostSummary> }

// 어떤 요청의 결과인지 key로 구분해서, 조건이 바뀌면 자동으로 로딩 상태가 돼요.
type ListResult = { key: string } & ({ kind: 'error'; error: ApiError } | { kind: 'success'; data: PageResponse<StudyPostSummary> })

export default function StudyListPage() {
    // 검색어, 필터, 페이지는 URL에 저장해서 뒤로 가기와 새로고침에도 유지돼요.
    const [searchParams, setSearchParams] = useSearchParams()
    const keyword = searchParams.get('keyword')?.trim() ?? ''
    const status = parseStatus(searchParams.get('status'))
    const part = parsePart(searchParams.get('part'))
    const page = parsePage(searchParams.get('page'))

    const [keywordInput, setKeywordInput] = useState(keyword)
    const [syncedKeyword, setSyncedKeyword] = useState(keyword)
    const [result, setResult] = useState<ListResult | null>(null)
    const [reloadKey, setReloadKey] = useState(0)

    // URL이 바뀌면(뒤로 가기 등) 검색창도 맞춰요.
    if (syncedKeyword !== keyword) {
        setSyncedKeyword(keyword)
        setKeywordInput(keyword)
    }

    const requestKey = JSON.stringify([keyword, status, part, page, reloadKey])
    const state: ListState = result && result.key === requestKey ? result : { kind: 'loading' }

    useEffect(() => {
        let ignore = false
        getStudyPosts({ keyword: keyword || undefined, status, part, page, size: PAGE_SIZE })
            .then((data) => {
                if (!ignore) setResult({ key: requestKey, kind: 'success', data })
            })
            .catch((error) => {
                if (!ignore) setResult({ key: requestKey, kind: 'error', error: toApiError(error) })
            })
        return () => {
            ignore = true
        }
    }, [keyword, status, part, page, requestKey])

    // 필터를 바꾸면 첫 페이지로 돌아가요.
    const updateParams = (changes: Record<string, string | undefined>, resetPage = true) => {
        const next = new URLSearchParams(searchParams)
        Object.entries(changes).forEach(([key, value]) => {
            if (value) next.set(key, value)
            else next.delete(key)
        })
        if (resetPage) next.delete('page')
        setSearchParams(next)
    }

    const handleSearch = (e: FormEvent) => {
        e.preventDefault()
        updateParams({ keyword: keywordInput.trim() || undefined })
    }

    const handlePageChange = (nextPage: number) => {
        updateParams({ page: nextPage > 0 ? String(nextPage + 1) : undefined }, false)
        window.scrollTo({ top: 0, behavior: 'smooth' })
    }

    const hasFilter = !!keyword || !!status || !!part

    return (
        <div className="mx-auto w-full max-w-4xl">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
                <div>
                    <h2 className="text-2xl font-bold text-slate-900">스터디 모집글</h2>
                    <p className="mt-1 text-sm text-slate-500">함께 공부할 스터디를 찾아보세요.</p>
                </div>
                <Link to="/studies/new" className={primaryButtonClass}>
                    + 글 작성
                </Link>
            </div>

            <div className="mt-6 rounded-xl bg-white p-4 shadow-sm">
                <form className="flex gap-2" onSubmit={handleSearch} role="search">
                    <input
                        className={inputClass}
                        type="search"
                        placeholder="제목이나 내용으로 검색해요"
                        value={keywordInput}
                        onChange={(e) => setKeywordInput(e.target.value)}
                        aria-label="검색어"
                    />
                    <button type="submit" className={`${primaryButtonClass} shrink-0`}>
                        검색
                    </button>
                </form>
                <div className="mt-3 grid grid-cols-2 gap-2 sm:flex sm:items-center">
                    <select
                        className={`${inputClass} sm:w-40`}
                        value={status ?? ''}
                        onChange={(e) => updateParams({ status: e.target.value || undefined })}
                        aria-label="모집 상태"
                    >
                        <option value="">전체 상태</option>
                        {STUDY_STATUSES.map((s) => (
                            <option key={s} value={s}>
                                {STUDY_STATUS_LABELS[s]}
                            </option>
                        ))}
                    </select>
                    <select
                        className={`${inputClass} sm:w-44`}
                        value={part ?? ''}
                        onChange={(e) => updateParams({ part: e.target.value || undefined })}
                        aria-label="작성자 파트"
                    >
                        <option value="">전체 파트</option>
                        {PARTS.map((p) => (
                            <option key={p} value={p}>
                                {PART_LABELS[p]}
                            </option>
                        ))}
                    </select>
                    {hasFilter && (
                        <button
                            type="button"
                            className={`${secondaryButtonClass} col-span-2 sm:col-span-1`}
                            onClick={() => setSearchParams(new URLSearchParams())}
                        >
                            초기화
                        </button>
                    )}
                </div>
            </div>

            <div className="mt-4">
                {state.kind === 'loading' && <LoadingState message="모집글을 불러오고 있어요..." />}

                {state.kind === 'error' && (
                    <MessageState
                        tone="error"
                        title="모집글을 불러오지 못했어요."
                        description={state.error.message}
                        action={
                            <button
                                type="button"
                                className={secondaryButtonClass}
                                onClick={() => setReloadKey((k) => k + 1)}
                            >
                                다시 시도
                            </button>
                        }
                    />
                )}

                {state.kind === 'success' &&
                    state.data.content.length === 0 &&
                    state.data.totalElements > 0 && (
                        <MessageState
                            title="존재하지 않는 페이지예요."
                            action={
                                <button
                                    type="button"
                                    className={secondaryButtonClass}
                                    onClick={() => handlePageChange(0)}
                                >
                                    첫 페이지로
                                </button>
                            }
                        />
                    )}

                {state.kind === 'success' && state.data.totalElements === 0 && (
                    <MessageState
                        title={hasFilter ? '조건에 맞는 모집글이 없어요.' : '아직 모집글이 없어요.'}
                        description={
                            hasFilter ? '검색어나 필터를 바꿔 보세요.' : '첫 번째 스터디 모집글을 작성해 보세요.'
                        }
                        action={
                            <Link to="/studies/new" className={primaryButtonClass}>
                                글 작성하기
                            </Link>
                        }
                    />
                )}

                {state.kind === 'success' && state.data.content.length > 0 && (
                    <>
                        <p className="mb-2 text-sm text-slate-500">
                            총 <span className="font-semibold text-slate-700">{state.data.totalElements}</span>개
                        </p>
                        <ul className="flex flex-col gap-3">
                            {state.data.content.map((post) => (
                                <li key={post.postId}>
                                    <Link
                                        to={`/studies/${post.postId}`}
                                        className="block rounded-xl bg-white p-4 shadow-sm ring-1 ring-transparent transition hover:ring-blue-200 sm:p-5"
                                    >
                                        <div className="flex items-start gap-2">
                                            <StatusBadge status={post.studyStatus} />
                                            <h3 className="min-w-0 flex-1 break-words font-semibold text-slate-900">
                                                {post.title}
                                            </h3>
                                        </div>
                                        <div className="mt-3 flex flex-wrap items-center justify-between gap-2">
                                            <AuthorInfo author={post.author} />
                                            <div className="flex items-center gap-3 text-xs text-slate-500">
                                                <span>댓글 {post.commentCount}</span>
                                                <time dateTime={post.createdAt}>{formatDate(post.createdAt)}</time>
                                            </div>
                                        </div>
                                    </Link>
                                </li>
                            ))}
                        </ul>
                        <Pagination
                            page={state.data.page}
                            totalPages={state.data.totalPages}
                            onChange={handlePageChange}
                        />
                    </>
                )}
            </div>
        </div>
    )
}
