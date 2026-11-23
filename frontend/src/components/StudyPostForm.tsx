import { useState, type FormEvent, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { getErrorMessage } from '../api/error'
import { STUDY_STATUSES, STUDY_STATUS_LABELS, type StudyStatus } from '../api/types'
import { ErrorAlert } from './ErrorAlert'
import { SelectField, SubmitButton, TextAreaField, TextField } from './FormField'

const TITLE_MAX = 50
const CONTENT_MAX = 2000

export interface StudyPostFormValues {
  title: string
  content: string
  studyStatus: StudyStatus
}

interface StudyPostFormProps {
  initialValues: StudyPostFormValues
  // 수정 화면에서만 모집 상태를 고를 수 있어요 (새 글은 항상 모집 중)
  showStatus: boolean
  submitLabel: string
  loadingText: string
  cancelTo: string
  onSubmit: (values: StudyPostFormValues) => Promise<void>
}

interface FormErrors {
  title?: string
  content?: string
}

// 서버(api-spec 5·8번)와 같은 규칙: 제목 1~50자, 내용 1~2000자, 공백만 있으면 안 돼요
function validate(values: StudyPostFormValues): FormErrors {
  const errors: FormErrors = {}
  const title = values.title.trim()
  const content = values.content.trim()
  if (!title) errors.title = '제목을 입력해 주세요.'
  else if (title.length > TITLE_MAX) errors.title = `제목은 ${TITLE_MAX}자 이하로 입력해 주세요.`
  if (!content) errors.content = '내용을 입력해 주세요.'
  else if (content.length > CONTENT_MAX) errors.content = `내용은 ${CONTENT_MAX}자 이하로 입력해 주세요.`
  return errors
}

export function StudyPostForm({
  initialValues,
  showStatus,
  submitLabel,
  loadingText,
  cancelTo,
  onSubmit,
}: StudyPostFormProps) {
  const [values, setValues] = useState<StudyPostFormValues>(initialValues)
  const [errors, setErrors] = useState<FormErrors>({})
  const [serverError, setServerError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    if (loading) return
    setServerError(null)

    const nextErrors = validate(values)
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) return

    setLoading(true)
    try {
      await onSubmit({ ...values, title: values.title.trim(), content: values.content.trim() })
      // 성공하면 부모가 다른 화면으로 이동시켜서 loading을 되돌리지 않아요
    } catch (error) {
      setServerError(getErrorMessage(error))
      setLoading(false)
    }
  }

  // maxLength를 걸지 않아요. 붙여넣은 긴 글이 몰래 잘리지 않고 글자 수와 메시지로 알려주려고요
  return (
    <form noValidate onSubmit={handleSubmit} className="space-y-5">
      <ErrorAlert message={serverError} />
      <TextField
        label="제목"
        name="title"
        placeholder="예: 알고리즘 스터디 모집"
        maxCount={TITLE_MAX}
        value={values.title}
        error={errors.title}
        onChange={(e) => {
          setValues((prev) => ({ ...prev, title: e.target.value }))
          setErrors((prev) => ({ ...prev, title: undefined }))
        }}
      />
      <TextAreaField
        label="내용"
        name="content"
        placeholder="스터디 주제, 진행 방식, 일정, 모집 인원 등을 적어 주세요."
        maxCount={CONTENT_MAX}
        value={values.content}
        error={errors.content}
        onChange={(e) => {
          setValues((prev) => ({ ...prev, content: e.target.value }))
          setErrors((prev) => ({ ...prev, content: undefined }))
        }}
      />
      {showStatus && (
        <SelectField
          label="모집 상태"
          name="studyStatus"
          value={values.studyStatus}
          onChange={(e) => setValues((prev) => ({ ...prev, studyStatus: e.target.value as StudyStatus }))}
        >
          {STUDY_STATUSES.map((status) => (
            <option key={status} value={status}>
              {STUDY_STATUS_LABELS[status]}
            </option>
          ))}
        </SelectField>
      )}
      <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
        <Link
          to={cancelTo}
          className="rounded-md border border-slate-300 bg-white px-4 py-2.5 text-center text-base font-medium text-slate-700 hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-500"
        >
          취소
        </Link>
        <div className="sm:w-40">
          <SubmitButton loading={loading} loadingText={loadingText}>
            {submitLabel}
          </SubmitButton>
        </div>
      </div>
    </form>
  )
}

// 작성·수정 화면이 같이 쓰는 카드 레이아웃
export function StudyFormLayout({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="mx-auto w-full max-w-3xl rounded-xl border border-slate-200 bg-white p-5 shadow-sm sm:p-8">
      <h1 className="mb-6 text-2xl font-bold text-slate-900">{title}</h1>
      {children}
    </section>
  )
}
