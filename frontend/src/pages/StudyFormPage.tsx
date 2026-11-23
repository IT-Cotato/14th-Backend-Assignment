import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'

import { getErrorMessage } from '../api/error'
import { createStudyPost, getStudyPost, updateStudyPost } from '../api/studyPosts'
import { STATUS_OPTIONS, type StudyStatus } from '../api/types'
import { ErrorMessage, Loading } from '../components/StatusMessage'

const TITLE_MAX = 50
const CONTENT_MAX = 2000

interface FieldErrors {
  title?: string
  content?: string
}

const fieldClass =
  'mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-gray-900 focus:outline-none'

// /studies/new (작성) 과 /studies/:id/edit (수정) 공용 화면
// 수정 화면에서는 모집 상태도 바꿀 수 있어요.
export function StudyFormPage() {
  const { id } = useParams()
  const isEdit = id !== undefined
  const postId = Number(id)
  const navigate = useNavigate()

  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [studyStatus, setStudyStatus] = useState<StudyStatus>('RECRUITING')
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [serverError, setServerError] = useState<string | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [isLoaded, setIsLoaded] = useState(!isEdit)
  const [isSubmitting, setIsSubmitting] = useState(false)

  // 수정 화면이면 기존 내용을 불러와요
  useEffect(() => {
    if (!isEdit) return
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
      .catch((err: unknown) => {
        if (!cancelled) setLoadError(getErrorMessage(err))
      })
    return () => {
      cancelled = true
    }
  }, [isEdit, postId])

  const validate = (): FieldErrors => {
    const errors: FieldErrors = {}
    const trimmedTitle = title.trim()
    if (!trimmedTitle || trimmedTitle.length > TITLE_MAX) {
      errors.title = `제목은 1~${TITLE_MAX}자로 입력해 주세요.`
    }
    const trimmedContent = content.trim()
    if (!trimmedContent || trimmedContent.length > CONTENT_MAX) {
      errors.content = `내용은 1~${CONTENT_MAX}자로 입력해 주세요.`
    }
    return errors
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setServerError(null)

    const errors = validate()
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) return

    setIsSubmitting(true)
    try {
      const request = { title: title.trim(), content: content.trim() }
      const { postId: savedId } = isEdit
        ? await updateStudyPost(postId, { ...request, studyStatus })
        : await createStudyPost(request)
      navigate(`/studies/${savedId}`, { replace: true })
    } catch (err) {
      setServerError(getErrorMessage(err))
      setIsSubmitting(false)
    }
  }

  if (loadError) {
    return (
      <section className="space-y-4">
        <ErrorMessage message={loadError} />
        <Link to="/studies" className="text-sm text-gray-700 underline">
          목록으로
        </Link>
      </section>
    )
  }
  if (!isLoaded) {
    return <Loading />
  }

  return (
    <section className="mx-auto max-w-2xl">
      <h1 className="mb-6 text-2xl font-bold">{isEdit ? '모집글 수정' : '모집글 작성'}</h1>

      <form onSubmit={handleSubmit} noValidate className="space-y-4">
        {serverError && <ErrorMessage message={serverError} />}

        <div>
          <label htmlFor="title" className="text-sm font-medium">
            제목
          </label>
          <input
            id="title"
            type="text"
            value={title}
            maxLength={TITLE_MAX}
            onChange={(e) => setTitle(e.target.value)}
            aria-invalid={Boolean(fieldErrors.title)}
            aria-describedby={fieldErrors.title ? 'title-error' : undefined}
            className={fieldClass}
          />
          {fieldErrors.title && (
            <p id="title-error" className="mt-1 text-xs text-red-600">
              {fieldErrors.title}
            </p>
          )}
        </div>

        <div>
          <label htmlFor="content" className="text-sm font-medium">
            내용
          </label>
          <textarea
            id="content"
            rows={10}
            value={content}
            maxLength={CONTENT_MAX}
            onChange={(e) => setContent(e.target.value)}
            aria-invalid={Boolean(fieldErrors.content)}
            aria-describedby={fieldErrors.content ? 'content-error' : undefined}
            className={fieldClass}
          />
          <p className="mt-1 text-right text-xs text-gray-400">
            {content.length} / {CONTENT_MAX}
          </p>
          {fieldErrors.content && (
            <p id="content-error" className="mt-1 text-xs text-red-600">
              {fieldErrors.content}
            </p>
          )}
        </div>

        {isEdit && (
          <div>
            <label htmlFor="studyStatus" className="text-sm font-medium">
              모집 상태
            </label>
            <select
              id="studyStatus"
              value={studyStatus}
              onChange={(e) => setStudyStatus(e.target.value as StudyStatus)}
              className={fieldClass}
            >
              {STATUS_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>
        )}

        <div className="flex gap-2">
          <button
            type="submit"
            disabled={isSubmitting}
            className="flex-1 rounded-md bg-gray-900 py-2 text-sm font-medium text-white hover:bg-gray-700 disabled:opacity-50"
          >
            {isSubmitting ? '저장하는 중...' : isEdit ? '수정하기' : '작성하기'}
          </button>
          <Link
            to={isEdit ? `/studies/${postId}` : '/studies'}
            className="rounded-md border border-gray-300 px-4 py-2 text-sm hover:bg-gray-50"
          >
            취소
          </Link>
        </div>
      </form>
    </section>
  )
}
