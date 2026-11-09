import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { getStudyPost, updateStudyPost } from '../api/studyPosts'
import { toApiError, type ApiError } from '../api/errors'
import { secondaryButtonClass } from '../components/FormField'
import LoadingState from '../components/LoadingState'
import MessageState from '../components/MessageState'
import StudyPostForm, { type StudyPostFormValues } from '../components/StudyPostForm'
import type { StudyPostDetail, StudyPostUpdateRequest } from '../types/studyPost'
import { parsePostId } from '../utils/params'

type EditState =
    | { kind: 'loading' }
    | { kind: 'error'; error: ApiError }
    | { kind: 'forbidden' }
    | { kind: 'ready'; post: StudyPostDetail }

// 바뀐 필드만 PATCH로 보내요.
function diff(post: StudyPostDetail, values: StudyPostFormValues): StudyPostUpdateRequest {
    const request: StudyPostUpdateRequest = {}
    if (values.title !== post.title) request.title = values.title
    if (values.content !== post.content) request.content = values.content
    if (values.studyStatus !== post.studyStatus) request.studyStatus = values.studyStatus
    return request
}

function ForbiddenState({ postId }: { postId: number }) {
    return (
        <MessageState
            tone="error"
            title="수정 권한이 없어요."
            description="작성자 본인만 모집글을 수정할 수 있어요."
            action={
                <Link to={`/studies/${postId}`} className={secondaryButtonClass}>
                    모집글로 돌아가기
                </Link>
            }
        />
    )
}

export default function StudyEditPage() {
    const { id } = useParams()
    const postId = parsePostId(id)
    const navigate = useNavigate()

    const [result, setResult] = useState<({ postId: number } & EditState) | null>(null)
    const [submitting, setSubmitting] = useState(false)
    const [errorMessage, setErrorMessage] = useState<string | null>(null)

    // 다른 글의 결과가 남아 있으면 로딩 상태로 봐요.
    const state: EditState = result && result.postId === postId ? result : { kind: 'loading' }
    const setState = (next: EditState) => {
        if (postId !== null) setResult({ postId, ...next })
    }

    useEffect(() => {
        if (postId === null) return
        let ignore = false
        const setState = (next: EditState) => setResult({ postId, ...next })
        getStudyPost(postId)
            .then((post) => {
                if (ignore) return
                // 상세 응답의 isAuthor로 작성자 여부를 먼저 확인해요. 서버도 PATCH에서 한 번 더 검증해요.
                setState(post.isAuthor ? { kind: 'ready', post } : { kind: 'forbidden' })
            })
            .catch((error) => {
                if (!ignore) setState({ kind: 'error', error: toApiError(error) })
            })
        return () => {
            ignore = true
        }
    }, [postId])

    if (postId === null || (state.kind === 'error' && state.error.status === 404)) {
        return (
            <MessageState
                title="존재하지 않는 모집글이에요."
                action={
                    <Link to="/studies" className={secondaryButtonClass}>
                        목록으로
                    </Link>
                }
            />
        )
    }

    if (state.kind === 'loading') return <LoadingState message="모집글을 불러오고 있어요..." />
    if (state.kind === 'forbidden') return <ForbiddenState postId={postId} />
    if (state.kind === 'error') {
        return <MessageState tone="error" title="모집글을 불러오지 못했어요." description={state.error.message} />
    }

    const { post } = state

    const handleSubmit = async (values: StudyPostFormValues) => {
        const request = diff(post, values)
        if (Object.keys(request).length === 0) {
            navigate(`/studies/${post.postId}`)
            return
        }
        setSubmitting(true)
        setErrorMessage(null)
        try {
            await updateStudyPost(post.postId, request)
            navigate(`/studies/${post.postId}`, { replace: true })
        } catch (error) {
            const apiError = toApiError(error)
            if (apiError.status === 403) {
                setState({ kind: 'forbidden' })
                return
            }
            if (apiError.status === 404) {
                setState({ kind: 'error', error: apiError })
                return
            }
            setErrorMessage(apiError.message)
            setSubmitting(false)
        }
    }

    return (
        <div className="mx-auto w-full max-w-3xl rounded-xl bg-white p-5 shadow-sm sm:p-8">
            <h2 className="mb-6 text-2xl font-bold text-slate-900">스터디 모집글 수정</h2>
            <StudyPostForm
                initialValues={{ title: post.title, content: post.content, studyStatus: post.studyStatus }}
                showStatus
                submitLabel="수정 완료"
                submitting={submitting}
                errorMessage={errorMessage}
                onSubmit={handleSubmit}
                onCancel={() => navigate(`/studies/${post.postId}`)}
            />
        </div>
    )
}
