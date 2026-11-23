import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { createStudy, fetchStudy, updateStudy } from '../api/studies'
import { STUDY_STATUSES, type StudyStatus } from '../api/types'
import { SelectField, TextAreaField, TextField } from '../components/FormFields'
import { cardClass, primaryButtonClass } from '../components/styles'
import { getErrorMessage } from '../utils/messages'

interface StudyForm {
  title: string
  content: string
  studyStatus: StudyStatus
}

type FormErrors = Partial<Record<keyof StudyForm, string>>

// 서버 검증 규칙(docs/api-spec.md)과 같은 기준으로 먼저 확인한다.
function validate(form: StudyForm): FormErrors {
  const errors: FormErrors = {}
  if (form.title.trim().length < 1 || form.title.length > 50) {
    errors.title = '제목은 1자 이상 50자 이하로 입력해 주세요.'
  }
  if (form.content.trim().length < 1 || form.content.length > 2000) {
    errors.content = '내용은 1자 이상 2000자 이하로 입력해 주세요.'
  }
  return errors
}

// /studies/new 와 /studies/:id/edit 를 함께 처리한다. id가 있으면 수정 화면이다.
export function StudyFormPage() {
  const { id } = useParams()
  const isEdit = id !== undefined
  const postId = Number(id)
  const invalidId = isEdit && !Number.isInteger(postId)
  const navigate = useNavigate()

  const [form, setForm] = useState<StudyForm>({ title: '', content: '', studyStatus: 'RECRUITING' })
  const [errors, setErrors] = useState<FormErrors>({})
  const [fetchError, setFetchError] = useState<string | null>(null)
  const [serverError, setServerError] = useState<string | null>(null)
  const [loading, setLoading] = useState(isEdit && !invalidId)
  const [submitting, setSubmitting] = useState(false)

  const loadError = invalidId ? '올바르지 않은 모집글 주소예요.' : fetchError

  // 수정 화면에서는 기존 내용을 불러와 채운다
  useEffect(() => {
    if (!isEdit || invalidId) {
      return
    }
    let cancelled = false
    fetchStudy(postId)
      .then((post) => {
        if (cancelled) return
        if (!post.isAuthor) {
          setFetchError('작성자만 모집글을 수정할 수 있어요.')
          return
        }
        setForm({ title: post.title, content: post.content, studyStatus: post.studyStatus })
      })
      .catch((err) => {
        if (!cancelled) setFetchError(getErrorMessage(err))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [isEdit, invalidId, postId])

  function handleChange(event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) {
    const { name, value } = event.target
    setForm((prev) => ({ ...prev, [name]: value }))
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setServerError(null)

    const found = validate(form)
    setErrors(found)
    if (Object.keys(found).length > 0) {
      return
    }

    setSubmitting(true)
    try {
      if (isEdit) {
        await updateStudy(postId, { title: form.title, content: form.content, studyStatus: form.studyStatus })
        navigate(`/studies/${postId}`, { replace: true })
      } else {
        const created = await createStudy({ title: form.title, content: form.content })
        navigate(`/studies/${created.postId}`, { replace: true })
      }
    } catch (err) {
      setServerError(getErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return <p className="py-10 text-center text-slate-500">불러오는 중이에요...</p>
  }
  if (loadError) {
    return (
      <div className="text-center">
        <p role="alert" className="mb-4 text-red-600">
          {loadError}
        </p>
        <Link to="/studies" className="text-indigo-600 hover:underline">
          스터디 목록으로 돌아가기
        </Link>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-xl">
      <h1 className="mb-6 text-2xl font-bold">{isEdit ? '모집글 수정' : '모집글 작성'}</h1>

      {/* noValidate: 브라우저 기본 검증 대신 위의 validate가 만든 안내 문구를 보여준다 */}
      <form onSubmit={handleSubmit} noValidate className={`${cardClass} space-y-4`}>
        <TextField
          label="제목"
          type="text"
          name="title"
          value={form.title}
          onChange={handleChange}
          error={errors.title}
        />
        <TextAreaField
          label="내용"
          name="content"
          rows={10}
          value={form.content}
          onChange={handleChange}
          error={errors.content}
        />
        {isEdit && (
          <SelectField label="모집 상태" name="studyStatus" value={form.studyStatus} onChange={handleChange}>
            {STUDY_STATUSES.map((s) => (
              <option key={s.value} value={s.value}>
                {s.label}
              </option>
            ))}
          </SelectField>
        )}

        {serverError && (
          <p role="alert" className="text-sm text-red-600">
            {serverError}
          </p>
        )}

        <button type="submit" disabled={submitting} className={primaryButtonClass}>
          {submitting ? '저장 중...' : isEdit ? '수정 완료' : '등록'}
        </button>
      </form>
    </div>
  )
}
