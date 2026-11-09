import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { deleteStudyPost, getStudyPost } from '../api/studyPosts'
import { toApiError, type ApiError } from '../api/errors'
import AuthorInfo from '../components/AuthorInfo'
import CommentSection from '../components/CommentSection'
import {
    FormError,
    dangerButtonClass,
    secondaryButtonClass,
} from '../components/FormField'
import LoadingState from '../components/LoadingState'
import MessageState from '../components/MessageState'
import StatusBadge from '../components/StatusBadge'
import type { StudyPostDetail } from '../types/studyPost'
import { formatDateTime } from '../utils/format'
import { parsePostId } from '../utils/params'
import { useAuth } from '../auth/AuthContext'

type DetailState =
    | { kind: 'loading' }
    | { kind: 'error'; error: ApiError }
    | { kind: 'success'; post: StudyPostDetail }

export default function StudyDetailPage() {
    const { id } = useParams()
    const postId = parsePostId(id)
    const navigate = useNavigate()
    const { user, initializing } = useAuth()

    const [result, setResult] = useState<({ key: string } & DetailState) | null>(null)
    const [reloadKey, setReloadKey] = useState(0)
    const [deleting, setDeleting] = useState(false)
    const [deleteError, setDeleteError] = useState<string | null>(null)

    // 로그인 상태가 바뀌면 isAuthor가 달라지니 다시 불러와요.
    // 저장된 토큰을 확인하는 동안에는 기다렸다가 한 번만 불러와요.
    const userId = user?.userId
    const requestKey = JSON.stringify([postId, userId, reloadKey])
    const state: DetailState = result && result.key === requestKey ? result : { kind: 'loading' }

    useEffect(() => {
        if (postId === null || initializing) return
        let ignore = false
        getStudyPost(postId)
            .then((post) => {
                if (!ignore) setResult({ key: requestKey, kind: 'success', post })
            })
            .catch((error) => {
                if (!ignore) setResult({ key: requestKey, kind: 'error', error: toApiError(error) })
            })
        return () => {
            ignore = true
        }
    }, [postId, initializing, requestKey])

    if (postId === null || (state.kind === 'error' && state.error.status === 404)) {
        return (
            <MessageState
                title="존재하지 않는 모집글이에요."
                description="삭제되었거나 주소가 잘못되었을 수 있어요."
                action={
                    <Link to="/studies" className={secondaryButtonClass}>
                        목록으로
                    </Link>
                }
            />
        )
    }

    if (state.kind === 'loading') {
        return <LoadingState message="모집글을 불러오고 있어요..." />
    }

    if (state.kind === 'error') {
        return (
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
        )
    }

    const { post } = state

    const handleDelete = async () => {
        if (!window.confirm('이 모집글을 삭제할까요? 삭제한 글과 댓글은 되돌릴 수 없어요.')) return
        setDeleting(true)
        setDeleteError(null)
        try {
            await deleteStudyPost(post.postId)
            navigate('/studies', { replace: true })
        } catch (error) {
            const apiError = toApiError(error)
            // 401은 인터셉터가 로그인 화면으로 보내요.
            if (apiError.status !== 401) {
                setDeleteError(
                    apiError.status === 403 ? '작성자 본인만 삭제할 수 있어요.' : apiError.message,
                )
            }
            setDeleting(false)
        }
    }

    const edited = post.updatedAt !== post.createdAt

    return (
        <div className="mx-auto w-full max-w-3xl">
            <Link to="/studies" className="text-sm text-slate-500 hover:text-slate-700">
                ← 목록으로
            </Link>

            <article className="mt-3 rounded-xl bg-white p-5 shadow-sm sm:p-8">
                <div className="flex items-start gap-2">
                    <StatusBadge status={post.studyStatus} />
                </div>
                <h2 className="mt-3 break-words text-2xl font-bold text-slate-900">{post.title}</h2>

                <div className="mt-4 flex flex-col gap-2 border-b border-slate-100 pb-4 sm:flex-row sm:items-center sm:justify-between">
                    <AuthorInfo author={post.author} />
                    <div className="flex flex-wrap gap-x-3 text-xs text-slate-500">
                        <span>작성 {formatDateTime(post.createdAt)}</span>
                        {edited && <span>수정 {formatDateTime(post.updatedAt)}</span>}
                    </div>
                </div>

                <p className="mt-6 whitespace-pre-wrap break-words leading-relaxed text-slate-800">
                    {post.content}
                </p>

                {post.isAuthor && (
                    <div className="mt-8 flex flex-col gap-3 border-t border-slate-100 pt-4">
                        <FormError message={deleteError} />
                        <div className="flex justify-end gap-2">
                            <Link to={`/studies/${post.postId}/edit`} className={secondaryButtonClass}>
                                수정
                            </Link>
                            <button
                                type="button"
                                className={dangerButtonClass}
                                onClick={handleDelete}
                                disabled={deleting}
                            >
                                {deleting ? '삭제 중...' : '삭제'}
                            </button>
                        </div>
                    </div>
                )}
            </article>

            {/* 댓글 작성이 곧 스터디 신청이에요. */}
            <CommentSection postId={post.postId} studyStatus={post.studyStatus} />
        </div>
    )
}
