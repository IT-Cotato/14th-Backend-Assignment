import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { createComment, deleteComment, getComments, updateComment } from '../api/comments'
import { toApiError, type ApiError } from '../api/errors'
import { useAuth } from '../auth/AuthContext'
import AuthorInfo from './AuthorInfo'
import { FormError, inputClass, primaryButtonClass, secondaryButtonClass } from './FormField'
import LoadingState from './LoadingState'
import Pagination from './Pagination'
import type { PageResponse } from '../types/api'
import { COMMENT_MAX_LENGTH, COMMENT_PAGE_SIZE, type Comment } from '../types/comment'
import type { StudyStatus } from '../types/studyPost'
import { formatDateTime } from '../utils/format'

interface CommentSectionProps {
    postId: number
    studyStatus: StudyStatus
}

type ListState =
    | { kind: 'loading' }
    | { kind: 'error'; error: ApiError }
    | { kind: 'success'; data: PageResponse<Comment> }

// 댓글 내용 검증이에요. 앞뒤 공백을 뺀 길이가 1~500자여야 해요.
function validateContent(content: string): string | null {
    const trimmed = content.trim()
    if (!trimmed) return '댓글 내용을 입력해 주세요.'
    if (trimmed.length > COMMENT_MAX_LENGTH) return `댓글은 ${COMMENT_MAX_LENGTH}자 이하로 입력해 주세요.`
    return null
}

// 서버 오류를 상황에 맞는 안내 문구로 바꿔요. 401은 인터셉터가 로그인 화면으로 보내서 null이에요.
function commentErrorMessage(error: ApiError, action: '작성' | '수정' | '삭제'): string | null {
    switch (error.status) {
        case 401:
            return null
        case 403:
            return `작성자 본인만 댓글을 ${action}할 수 있어요.`
        case 404:
            return action === '작성'
                ? '모집글이 삭제되어 댓글을 작성할 수 없어요.'
                : '이미 삭제된 댓글이에요. 목록을 새로 불러왔어요.'
        case 409:
            return '모집이 마감되어 댓글을 작성할 수 없어요.'
        default:
            return error.message
    }
}

function CharacterCount({ value }: { value: string }) {
    const length = value.trim().length
    return (
        <span className={`text-xs ${length > COMMENT_MAX_LENGTH ? 'text-red-500' : 'text-slate-400'}`}>
            {length}/{COMMENT_MAX_LENGTH}
        </span>
    )
}

interface CommentItemProps {
    comment: Comment
    editing: boolean
    onStartEdit: () => void
    onCancelEdit: () => void
    onChanged: () => void
}

function CommentItem({ comment, editing, onStartEdit, onCancelEdit, onChanged }: CommentItemProps) {
    const [draft, setDraft] = useState(comment.content)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const startEdit = () => {
        setDraft(comment.content)
        setError(null)
        onStartEdit()
    }

    const handleUpdate = async (e: FormEvent) => {
        e.preventDefault()
        const validationError = validateContent(draft)
        if (validationError) {
            setError(validationError)
            return
        }
        setBusy(true)
        setError(null)
        try {
            await updateComment(comment.commentId, { content: draft.trim() })
            onCancelEdit()
            onChanged()
        } catch (err) {
            const apiError = toApiError(err)
            setError(commentErrorMessage(apiError, '수정'))
            if (apiError.status === 404) onChanged()
        } finally {
            setBusy(false)
        }
    }

    const handleDelete = async () => {
        if (!window.confirm('이 댓글을 삭제할까요? 삭제하면 스터디 신청도 취소돼요.')) return
        setBusy(true)
        setError(null)
        try {
            await deleteComment(comment.commentId)
            onChanged()
        } catch (err) {
            const apiError = toApiError(err)
            setError(commentErrorMessage(apiError, '삭제'))
            if (apiError.status === 404) onChanged()
            setBusy(false)
        }
    }

    return (
        <li className="py-4" data-testid="comment-item">
            <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
                <div className="flex flex-wrap items-center gap-2">
                    <AuthorInfo author={comment.author} />
                    {comment.isAuthor && (
                        <span className="rounded-full bg-blue-50 px-2 py-0.5 text-xs font-semibold text-blue-700">
                            내 댓글
                        </span>
                    )}
                </div>
                <time className="text-xs text-slate-500" dateTime={comment.createdAt}>
                    {formatDateTime(comment.createdAt)}
                </time>
            </div>

            {editing ? (
                <form className="mt-3 flex flex-col gap-2" onSubmit={handleUpdate} noValidate>
                    <label htmlFor={`comment-edit-${comment.commentId}`} className="sr-only">
                        댓글 수정
                    </label>
                    <textarea
                        id={`comment-edit-${comment.commentId}`}
                        className={`${inputClass} min-h-24 resize-y`}
                        value={draft}
                        onChange={(e) => setDraft(e.target.value)}
                        aria-invalid={!!error}
                        disabled={busy}
                    />
                    <div className="flex items-center justify-between gap-2">
                        <CharacterCount value={draft} />
                        <div className="flex gap-2">
                            <button
                                type="button"
                                className={secondaryButtonClass}
                                onClick={onCancelEdit}
                                disabled={busy}
                            >
                                취소
                            </button>
                            <button type="submit" className={primaryButtonClass} disabled={busy}>
                                {busy ? '저장 중...' : '저장'}
                            </button>
                        </div>
                    </div>
                    <FormError message={error} />
                </form>
            ) : (
                <>
                    <p className="mt-2 whitespace-pre-wrap break-words text-sm leading-relaxed text-slate-800">
                        {comment.content}
                    </p>
                    {comment.isAuthor && (
                        <div className="mt-2 flex flex-col gap-2">
                            <div className="flex justify-end gap-3 text-sm">
                                <button
                                    type="button"
                                    className="font-medium text-slate-500 hover:text-slate-800 disabled:opacity-50"
                                    onClick={startEdit}
                                    disabled={busy}
                                >
                                    수정
                                </button>
                                <button
                                    type="button"
                                    className="font-medium text-red-500 hover:text-red-700 disabled:opacity-50"
                                    onClick={handleDelete}
                                    disabled={busy}
                                >
                                    {busy ? '삭제 중...' : '삭제'}
                                </button>
                            </div>
                            <FormError message={error} />
                        </div>
                    )}
                </>
            )}
        </li>
    )
}

// 스터디 상세 페이지의 댓글(스터디 신청) 영역이에요.
export default function CommentSection({ postId, studyStatus }: CommentSectionProps) {
    const { user, isLoggedIn, initializing } = useAuth()

    const [page, setPage] = useState(0)
    const [reloadKey, setReloadKey] = useState(0)
    const [result, setResult] = useState<({ key: string } & ListState) | null>(null)
    const [editingId, setEditingId] = useState<number | null>(null)

    const [content, setContent] = useState('')
    const [submitting, setSubmitting] = useState(false)
    const [formError, setFormError] = useState<string | null>(null)
    // 화면을 연 뒤 다른 곳에서 마감된 경우 서버 409로 알게 돼요.
    const [closedByServer, setClosedByServer] = useState(false)

    const closed = studyStatus === 'CLOSED' || closedByServer

    // 로그인 상태가 바뀌면 isAuthor가 달라지니 다시 불러와요.
    const userId = user?.userId
    const requestKey = JSON.stringify([postId, userId, page, reloadKey])
    const state: ListState = result && result.key === requestKey ? result : { kind: 'loading' }

    useEffect(() => {
        if (initializing) return
        let ignore = false
        getComments(postId, page, COMMENT_PAGE_SIZE)
            .then((data) => {
                if (ignore) return
                // 마지막 페이지의 댓글을 모두 지우면 남은 마지막 페이지로 이동해요.
                if (data.content.length === 0 && data.page > 0 && data.totalPages > 0) {
                    setPage(data.totalPages - 1)
                    return
                }
                setResult({ key: requestKey, kind: 'success', data })
            })
            .catch((error) => {
                if (!ignore) setResult({ key: requestKey, kind: 'error', error: toApiError(error) })
            })
        return () => {
            ignore = true
        }
    }, [postId, page, initializing, requestKey])

    const reload = () => setReloadKey((k) => k + 1)

    const handlePageChange = (next: number) => {
        setEditingId(null)
        setPage(next)
    }

    const handleCreate = async (e: FormEvent) => {
        e.preventDefault()
        const validationError = validateContent(content)
        if (validationError) {
            setFormError(validationError)
            return
        }
        setSubmitting(true)
        setFormError(null)
        try {
            await createComment(postId, { content: content.trim() })
            setContent('')
            // 오래된 순이라 새 댓글은 마지막 페이지에 있어요.
            const total = state.kind === 'success' ? state.data.totalElements : 0
            setPage(Math.floor(total / COMMENT_PAGE_SIZE))
            reload()
        } catch (error) {
            const apiError = toApiError(error)
            if (apiError.status === 409) setClosedByServer(true)
            setFormError(commentErrorMessage(apiError, '작성'))
        } finally {
            setSubmitting(false)
        }
    }

    const totalElements = state.kind === 'success' ? state.data.totalElements : null

    return (
        <section className="mt-4 rounded-xl bg-white p-5 shadow-sm sm:p-8" aria-labelledby="comments-title">
            <div className="flex items-baseline justify-between gap-2">
                <h3 id="comments-title" className="font-semibold text-slate-900">
                    스터디 신청 댓글
                    {totalElements !== null && (
                        <span className="ml-1.5 text-blue-600">{totalElements}</span>
                    )}
                </h3>
            </div>
            <p className="mt-1 text-sm text-slate-500">댓글을 남기면 이 스터디에 신청한 것으로 처리돼요.</p>

            {/* 목록 */}
            <div className="mt-4">
                {state.kind === 'loading' && <LoadingState message="댓글을 불러오고 있어요..." />}

                {state.kind === 'error' && (
                    <div
                        className="flex flex-col items-center gap-3 rounded-lg bg-red-50 px-4 py-8 text-center"
                        role="alert"
                    >
                        <p className="text-sm font-medium text-red-700">
                            {state.error.status === 404
                                ? '모집글을 찾을 수 없어 댓글을 불러오지 못했어요.'
                                : '댓글을 불러오지 못했어요.'}
                        </p>
                        {state.error.status !== 404 && (
                            <>
                                <p className="text-xs text-red-600">{state.error.message}</p>
                                <button type="button" className={secondaryButtonClass} onClick={reload}>
                                    다시 시도
                                </button>
                            </>
                        )}
                    </div>
                )}

                {state.kind === 'success' && state.data.content.length === 0 && (
                    <p className="rounded-lg bg-slate-50 px-4 py-8 text-center text-sm text-slate-500">
                        {closed ? '신청 댓글 없이 모집이 마감되었어요.' : '아직 신청 댓글이 없어요. 첫 번째로 신청해 보세요!'}
                    </p>
                )}

                {state.kind === 'success' && state.data.content.length > 0 && (
                    <>
                        <ul className="divide-y divide-slate-100 border-y border-slate-100">
                            {state.data.content.map((comment) => (
                                <CommentItem
                                    key={comment.commentId}
                                    comment={comment}
                                    editing={editingId === comment.commentId}
                                    onStartEdit={() => setEditingId(comment.commentId)}
                                    onCancelEdit={() => setEditingId(null)}
                                    onChanged={reload}
                                />
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

            {/* 작성 영역 */}
            <div className="mt-6">
                {initializing ? null : closed ? (
                    <div className="flex flex-col gap-2">
                        <p className="rounded-lg bg-slate-100 px-4 py-3 text-sm text-slate-600" role="status">
                            모집이 마감된 스터디라 댓글을 작성할 수 없어요.
                        </p>
                        {closedByServer && studyStatus !== 'CLOSED' && (
                            <p className="text-xs text-slate-500">
                                방금 모집이 마감되었어요. 페이지를 새로고침하면 최신 상태를 볼 수 있어요.
                            </p>
                        )}
                    </div>
                ) : !isLoggedIn ? (
                    <div className="flex flex-col items-start gap-3 rounded-lg bg-slate-50 px-4 py-4 sm:flex-row sm:items-center sm:justify-between">
                        <p className="text-sm text-slate-600">스터디에 신청하려면 로그인해 주세요.</p>
                        <Link
                            to="/login"
                            state={{ from: `/studies/${postId}` }}
                            className={primaryButtonClass}
                        >
                            로그인하고 신청하기
                        </Link>
                    </div>
                ) : (
                    <form className="flex flex-col gap-2" onSubmit={handleCreate} noValidate>
                        <label htmlFor="comment-content" className="text-sm font-medium text-slate-700">
                            신청 댓글 작성
                        </label>
                        <textarea
                            id="comment-content"
                            className={`${inputClass} min-h-24 resize-y`}
                            value={content}
                            onChange={(e) => setContent(e.target.value)}
                            aria-invalid={!!formError}
                            placeholder="자기소개와 참여하고 싶은 이유를 남겨 주세요."
                            disabled={submitting}
                        />
                        <div className="flex items-center justify-between gap-2">
                            <CharacterCount value={content} />
                            <button type="submit" className={primaryButtonClass} disabled={submitting}>
                                {submitting ? '등록 중...' : '댓글 등록'}
                            </button>
                        </div>
                        <FormError message={formError} />
                    </form>
                )}
            </div>
        </section>
    )
}
