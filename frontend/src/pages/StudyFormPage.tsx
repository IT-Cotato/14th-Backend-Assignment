import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { getErrorMessage } from '../api/errors'
import { createStudyPost, getStudyPost, updateStudyPost } from '../api/studyPosts'
import { STUDY_STATUS_LABELS, STUDY_STATUSES, type StudyStatus } from '../api/types'
import { ErrorAlert } from '../components/ErrorAlert'
import { FormField } from '../components/FormField'
import { LoadingStatus } from '../components/LoadingStatus'
import { errorId, inputClassName, primaryButtonClassName } from '../components/form'

const TITLE_MAX = 50
const CONTENT_MAX = 2000

interface FormErrors {
  title?: string
  content?: string
}

// 검증 규칙은 docs/api-spec.md 5번(title 1~50자, content 1~2000자)과 같습니다.
function validate(title: string, content: string): FormErrors {
  const errors: FormErrors = {}
  if (!title.trim()) {
    errors.title = '제목을 입력해 주세요.'
  } else if (title.length > TITLE_MAX) {
    errors.title = `제목은 ${TITLE_MAX}자 이하여야 해요.`
  }
  if (!content.trim()) {
    errors.content = '내용을 입력해 주세요.'
  } else if (content.length > CONTENT_MAX) {
    errors.content = `내용은 ${CONTENT_MAX}자 이하여야 해요.`
  }
  return errors
}

// /studies/new(작성)와 /studies/:id/edit(수정)이 함께 씁니다. RequireAuth 안에서만 렌더링됩니다.
export function StudyFormPage() {
  const { id } = useParams()
  const isEdit = id !== undefined
  const postId = Number(id)
  const navigate = useNavigate()

  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [studyStatus, setStudyStatus] = useState<StudyStatus>('RECRUITING')
  const [errors, setErrors] = useState<FormErrors>({})
  const [serverError, setServerError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isLoaded, setIsLoaded] = useState(!isEdit)
  const [loadError, setLoadError] = useState<string | null>(null)

  // 수정 화면이면 기존 값을 채웁니다. 작성자가 아니면 폼을 보여주지 않습니다.
  useEffect(() => {
    if (!isEdit) {
      return
    }
    let cancelled = false
    getStudyPost(postId)
      .then((post) => {
        if (cancelled) return
        if (!post.isAuthor) {
          setLoadError('모집글 작성자만 수정할 수 있어요.')
          return
        }
        setTitle(post.title)
        setContent(post.content)
        setStudyStatus(post.studyStatus)
        setIsLoaded(true)
      })
      .catch((error: unknown) => {
        if (!cancelled) setLoadError(getErrorMessage(error))
      })
    return () => {
      cancelled = true
    }
  }, [isEdit, postId])

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setServerError(null)
    const nextErrors = validate(title, content)
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) {
      return
    }

    setIsSubmitting(true)
    try {
      if (isEdit) {
        await updateStudyPost(postId, { title, content, studyStatus })
        navigate(`/studies/${postId}`)
      } else {
        const created = await createStudyPost({ title, content })
        navigate(`/studies/${created.postId}`)
      }
    } catch (error) {
      setServerError(getErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  const pageTitle = isEdit ? '모집글 수정' : '모집글 작성'

  return (
    <section className="mx-auto flex w-full max-w-2xl flex-col gap-6">
      <h1 className="text-2xl font-bold text-gray-900">{pageTitle}</h1>

      {isEdit && !isLoaded && !loadError && <LoadingStatus />}
      <ErrorAlert message={loadError} />

      {isLoaded && (
        <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4 rounded-lg bg-white p-6 shadow-sm">
          <FormField id="post-title" label="제목" error={errors.title}>
            <input
              id="post-title"
              type="text"
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              aria-invalid={errors.title ? true : undefined}
              aria-describedby={errors.title ? errorId('post-title') : undefined}
              className={inputClassName}
            />
          </FormField>

          <FormField id="post-content" label="내용" error={errors.content}>
            <textarea
              id="post-content"
              rows={10}
              value={content}
              onChange={(event) => setContent(event.target.value)}
              aria-invalid={errors.content ? true : undefined}
              aria-describedby={errors.content ? errorId('post-content') : undefined}
              className={inputClassName}
            />
          </FormField>

          {isEdit && (
            <FormField id="post-status" label="모집 상태">
              <select
                id="post-status"
                value={studyStatus}
                onChange={(event) => setStudyStatus(event.target.value as StudyStatus)}
                className={inputClassName}
              >
                {STUDY_STATUSES.map((value) => (
                  <option key={value} value={value}>
                    {STUDY_STATUS_LABELS[value]}
                  </option>
                ))}
              </select>
            </FormField>
          )}

          <ErrorAlert message={serverError} />

          <button type="submit" disabled={isSubmitting} className={primaryButtonClassName}>
            {isSubmitting ? '저장 중…' : isEdit ? '수정하기' : '작성하기'}
          </button>

          <Link to={isEdit ? `/studies/${postId}` : '/studies'} className="text-center text-sm text-gray-600 hover:underline">
            취소
          </Link>
        </form>
      )}
    </section>
  )
}
