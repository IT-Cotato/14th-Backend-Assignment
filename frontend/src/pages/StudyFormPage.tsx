import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { getErrorMessage } from '../api/client'
import { createStudyPost, fetchStudyPost, updateStudyPost } from '../api/studyPosts'
import { ErrorMessage } from '../components/ErrorMessage'
import { Loading } from '../components/Loading'
import {
  cardClass,
  inputClass,
  labelClass,
  primaryButtonClass,
  secondaryButtonClass,
} from '../components/styles'
import { STUDY_STATUS_LABELS, STUDY_STATUSES, type StudyPostDetail, type StudyStatus } from '../types'

/** 모집글 작성 화면 (/studies/new) */
export function StudyCreatePage() {
  return (
    <section className={cardClass}>
      <h1 className="mb-4 text-xl font-bold">모집글 작성</h1>
      <StudyForm />
    </section>
  )
}

/** 모집글 수정 화면 (/studies/:id/edit) */
export function StudyEditPage() {
  const postId = Number(useParams().id)

  const { data: post, isPending, isError, error } = useQuery({
    queryKey: ['studyPost', postId],
    queryFn: () => fetchStudyPost(postId),
  })

  return (
    <section className={cardClass}>
      <h1 className="mb-4 text-xl font-bold">모집글 수정</h1>
      {isPending && <Loading />}
      {isError && <ErrorMessage message={getErrorMessage(error)} />}
      {post && !post.isAuthor && (
        <div className="space-y-4">
          <ErrorMessage message="모집글 작성자만 수정할 수 있습니다." />
          <Link to={`/studies/${post.postId}`} className={secondaryButtonClass}>
            모집글로 돌아가기
          </Link>
        </div>
      )}
      {post && post.isAuthor && <StudyForm post={post} />}
    </section>
  )
}

/** post 가 있으면 수정, 없으면 새로 작성한다 */
function StudyForm({ post }: { post?: StudyPostDetail }) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [title, setTitle] = useState(post?.title ?? '')
  const [content, setContent] = useState(post?.content ?? '')
  const [studyStatus, setStudyStatus] = useState<StudyStatus>(post?.studyStatus ?? 'RECRUITING')

  const mutation = useMutation({
    mutationFn: () =>
      post
        ? updateStudyPost(post.postId, { title, content, studyStatus })
        : createStudyPost({ title, content }),
    onSuccess: async ({ postId }) => {
      await queryClient.invalidateQueries({ queryKey: ['studyPosts'] })
      await queryClient.invalidateQueries({ queryKey: ['studyPost', postId] })
      navigate(`/studies/${postId}`)
    },
  })

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    mutation.mutate()
  }

  const submitLabel = post ? '수정하기' : '작성하기'

  return (
    <form onSubmit={handleSubmit} noValidate className="space-y-4">
      <div>
        <label htmlFor="study-title" className={labelClass}>
          제목
        </label>
        <input
          id="study-title"
          type="text"
          maxLength={50}
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          className={inputClass}
        />
      </div>
      <div>
        <label htmlFor="study-content" className={labelClass}>
          내용
        </label>
        <textarea
          id="study-content"
          rows={10}
          maxLength={2000}
          value={content}
          onChange={(event) => setContent(event.target.value)}
          className={inputClass}
        />
      </div>
      {post && (
        <div>
          <label htmlFor="study-status" className={labelClass}>
            모집 상태
          </label>
          <select
            id="study-status"
            value={studyStatus}
            onChange={(event) =>
              setStudyStatus(
                STUDY_STATUSES.find((status) => status === event.target.value) ?? 'RECRUITING',
              )
            }
            className={inputClass}
          >
            {STUDY_STATUSES.map((status) => (
              <option key={status} value={status}>
                {STUDY_STATUS_LABELS[status]}
              </option>
            ))}
          </select>
        </div>
      )}
      <ErrorMessage message={mutation.isError ? getErrorMessage(mutation.error) : null} />
      <div className="flex flex-wrap gap-2">
        <button type="submit" disabled={mutation.isPending} className={primaryButtonClass}>
          {mutation.isPending ? '저장하는 중...' : submitLabel}
        </button>
        <Link to={post ? `/studies/${post.postId}` : '/studies'} className={secondaryButtonClass}>
          취소
        </Link>
      </div>
    </form>
  )
}
