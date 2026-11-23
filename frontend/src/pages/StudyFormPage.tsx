import { useEffect, useState } from 'react'
import type { ChangeEvent, FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getErrorMessage } from '../api/client'
import {
  STUDY_STATUSES,
  STUDY_STATUS_LABEL,
  createStudyPost,
  getStudyPost,
  updateStudyPost,
} from '../api/studyPosts'
import type { StudyStatus } from '../api/studyPosts'
import { ErrorMessage } from '../components/ErrorMessage'
import { FormField } from '../components/FormField'
import { cardClass, inputClass, submitClass } from '../components/styles'

const TITLE_MAX = 50
const CONTENT_MAX = 2000

interface FormState {
  title: string
  content: string
  studyStatus: StudyStatus
}

const emptyForm: FormState = { title: '', content: '', studyStatus: 'RECRUITING' }

export function StudyFormPage({ mode }: { mode: 'create' | 'edit' }) {
  const isEdit = mode === 'edit'
  const { id = '' } = useParams()
  const navigate = useNavigate()

  const [form, setForm] = useState<FormState>(emptyForm)
  // 수정 화면은 기존 글을 불러온 뒤에 폼을 보여준다
  const [loading, setLoading] = useState(isEdit)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [forbidden, setForbidden] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!isEdit) return
    let ignore = false
    getStudyPost(id)
      .then((post) => {
        if (ignore) return
        setForbidden(!post.isAuthor)
        setForm({ title: post.title, content: post.content, studyStatus: post.studyStatus })
      })
      .catch((err) => {
        if (!ignore) setLoadError(getErrorMessage(err))
      })
      .finally(() => {
        if (!ignore) setLoading(false)
      })
    return () => {
      ignore = true
    }
  }, [isEdit, id])

  const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value })
  }

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      if (isEdit) {
        await updateStudyPost(id, form)
        navigate(`/studies/${id}`, { replace: true })
      } else {
        const { postId } = await createStudyPost({ title: form.title, content: form.content })
        navigate(`/studies/${postId}`, { replace: true })
      }
    } catch (err) {
      setError(getErrorMessage(err))
      setSubmitting(false)
    }
  }

  const cancelTo = isEdit ? `/studies/${id}` : '/studies'
  const heading = isEdit ? '모집글 수정' : '모집글 작성'

  if (loading || loadError || forbidden) {
    return (
      <section className={cardClass}>
        <h1 className="mb-6 text-2xl font-bold text-gray-900">{heading}</h1>
        {loading && <p className="text-gray-500">불러오는 중...</p>}
        <ErrorMessage message={loadError ?? (forbidden ? '작성자만 수정할 수 있습니다.' : null)} />
        {!loading && (
          <Link to={loadError ? '/studies' : cancelTo}
            className="mt-4 inline-block text-sm font-medium text-indigo-600 hover:underline">
            {loadError ? '목록으로' : '글로 돌아가기'}
          </Link>
        )}
      </section>
    )
  }

  return (
    <section className={cardClass}>
      <h1 className="mb-6 text-2xl font-bold text-gray-900">{heading}</h1>
      <form noValidate onSubmit={handleSubmit} className="flex flex-col gap-4">
        <FormField id="study-title" label="제목">
          <input id="study-title" name="title" type="text" maxLength={TITLE_MAX}
            value={form.title} onChange={handleChange} className={inputClass} />
        </FormField>
        <FormField id="study-content" label="내용">
          <textarea id="study-content" name="content" rows={10} maxLength={CONTENT_MAX}
            aria-describedby="study-content-count"
            value={form.content} onChange={handleChange} className={`${inputClass} resize-y`} />
          <p id="study-content-count" className="text-right text-xs text-gray-500">
            {form.content.length} / {CONTENT_MAX}
          </p>
        </FormField>
        {isEdit && (
          <FormField id="study-status" label="모집 상태">
            <select id="study-status" name="studyStatus" value={form.studyStatus}
              onChange={handleChange} className={inputClass}>
              {STUDY_STATUSES.map((value) => (
                <option key={value} value={value}>
                  {STUDY_STATUS_LABEL[value]}
                </option>
              ))}
            </select>
          </FormField>
        )}

        <ErrorMessage message={error} />

        <div className="flex flex-col-reverse gap-2 sm:flex-row">
          <Link to={cancelTo}
            className="rounded-md border border-gray-300 px-4 py-2 text-center font-medium text-gray-700 hover:bg-gray-50 sm:w-32">
            취소
          </Link>
          <button type="submit" disabled={submitting} className={submitClass}>
            {submitting ? (isEdit ? '수정 중...' : '등록 중...') : isEdit ? '수정' : '등록'}
          </button>
        </div>
      </form>
    </section>
  )
}
