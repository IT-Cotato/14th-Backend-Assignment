import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getErrorMessage } from '../api/error'
import { getStudyPost, updateStudyPost } from '../api/studyPosts'
import type { StudyPostDetail } from '../api/types'
import { ErrorAlert } from '../components/ErrorAlert'
import { Loading } from '../components/Loading'
import { StudyFormLayout, StudyPostForm, type StudyPostFormValues } from '../components/StudyPostForm'
import { NOT_FOUND_MESSAGE, parsePostId } from './studyPostId'

interface EditState {
  postId: number
  post?: StudyPostDetail
  error?: string
}

export function StudyEditPage() {
  const { id } = useParams()
  const postId = parsePostId(id)
  const navigate = useNavigate()
  const [state, setState] = useState<EditState | null>(null)

  useEffect(() => {
    if (postId === null) return
    let cancelled = false
    getStudyPost(postId)
      .then((post) => {
        if (cancelled) return
        // 주소로 남의 글 수정 화면에 들어오면 상세로 돌려보내요. 저장하더라도 서버가 403으로 한 번 더 막아요
        if (!post.isAuthor) {
          navigate(`/studies/${postId}`, { replace: true, state: { notice: '작성자만 수정할 수 있어요.' } })
          return
        }
        setState({ postId, post })
      })
      .catch((error: unknown) => {
        if (!cancelled) setState({ postId, error: getErrorMessage(error) })
      })
    return () => {
      cancelled = true
    }
  }, [postId, navigate])

  if (postId === null || (state?.postId === postId && !state.post)) {
    return (
      <div className="mx-auto w-full max-w-3xl space-y-4">
        <ErrorAlert message={state?.error ?? NOT_FOUND_MESSAGE} />
        <Link to="/studies" className="text-sm font-medium text-indigo-600 hover:underline">
          목록으로
        </Link>
      </div>
    )
  }
  if (state?.postId !== postId || !state.post) {
    return <Loading />
  }

  const post = state.post

  const handleSubmit = async (values: StudyPostFormValues) => {
    await updateStudyPost(post.postId, values)
    navigate(`/studies/${post.postId}`, { replace: true })
  }

  return (
    <StudyFormLayout title="모집글 수정">
      <StudyPostForm
        initialValues={{ title: post.title, content: post.content, studyStatus: post.studyStatus }}
        showStatus
        submitLabel="저장"
        loadingText="저장 중..."
        cancelTo={`/studies/${post.postId}`}
        onSubmit={handleSubmit}
      />
    </StudyFormLayout>
  )
}
