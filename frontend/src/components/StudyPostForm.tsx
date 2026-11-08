import { useState, type FormEvent } from 'react'
import FormField, {
    FormError,
    inputClass,
    primaryButtonClass,
    secondaryButtonClass,
} from './FormField'
import {
    CONTENT_MAX_LENGTH,
    STUDY_STATUSES,
    STUDY_STATUS_LABELS,
    TITLE_MAX_LENGTH,
    type StudyStatus,
} from '../types/studyPost'

export interface StudyPostFormValues {
    title: string
    content: string
    studyStatus: StudyStatus
}

interface StudyPostFormProps {
    initialValues: StudyPostFormValues
    // 수정 화면에서만 모집 상태를 바꿀 수 있어요.
    showStatus?: boolean
    submitLabel: string
    submitting: boolean
    errorMessage: string | null
    onSubmit: (values: StudyPostFormValues) => void
    onCancel: () => void
}

type FormErrors = Partial<Record<'title' | 'content', string>>

// docs/api-spec.md 5-1, 5-4의 검증 규칙(제목 1~50자, 내용 1~2000자)과 같아요.
function validate(values: StudyPostFormValues): FormErrors {
    const errors: FormErrors = {}
    const title = values.title.trim()
    const content = values.content.trim()
    if (!title) errors.title = '제목을 입력해 주세요.'
    else if (title.length > TITLE_MAX_LENGTH) errors.title = `제목은 ${TITLE_MAX_LENGTH}자 이하로 입력해 주세요.`
    if (!content) errors.content = '내용을 입력해 주세요.'
    else if (content.length > CONTENT_MAX_LENGTH)
        errors.content = `내용은 ${CONTENT_MAX_LENGTH}자 이하로 입력해 주세요.`
    return errors
}

export default function StudyPostForm({
    initialValues,
    showStatus = false,
    submitLabel,
    submitting,
    errorMessage,
    onSubmit,
    onCancel,
}: StudyPostFormProps) {
    const [values, setValues] = useState<StudyPostFormValues>(initialValues)
    const [errors, setErrors] = useState<FormErrors>({})

    const handleSubmit = (e: FormEvent) => {
        e.preventDefault()
        const nextErrors = validate(values)
        setErrors(nextErrors)
        if (Object.keys(nextErrors).length > 0) return
        onSubmit({ ...values, title: values.title.trim(), content: values.content.trim() })
    }

    const countClass = (length: number, max: number) => (length > max ? 'text-red-500' : undefined)

    return (
        <form className="flex flex-col gap-5" onSubmit={handleSubmit} noValidate>
            <FormField
                label="제목"
                htmlFor="title"
                error={errors.title}
                hint={
                    <span className={countClass(values.title.trim().length, TITLE_MAX_LENGTH)}>
                        {values.title.trim().length}/{TITLE_MAX_LENGTH}
                    </span>
                }
            >
                <input
                    id="title"
                    className={inputClass}
                    value={values.title}
                    onChange={(e) => setValues((v) => ({ ...v, title: e.target.value }))}
                    aria-invalid={!!errors.title}
                    placeholder="예) 알고리즘 스터디 모집"
                />
            </FormField>

            {showStatus && (
                <FormField label="모집 상태" htmlFor="studyStatus">
                    <select
                        id="studyStatus"
                        className={inputClass}
                        value={values.studyStatus}
                        onChange={(e) =>
                            setValues((v) => ({ ...v, studyStatus: e.target.value as StudyStatus }))
                        }
                    >
                        {STUDY_STATUSES.map((s) => (
                            <option key={s} value={s}>
                                {STUDY_STATUS_LABELS[s]}
                            </option>
                        ))}
                    </select>
                </FormField>
            )}

            <FormField
                label="내용"
                htmlFor="content"
                error={errors.content}
                hint={
                    <span className={countClass(values.content.trim().length, CONTENT_MAX_LENGTH)}>
                        {values.content.trim().length}/{CONTENT_MAX_LENGTH}
                    </span>
                }
            >
                <textarea
                    id="content"
                    className={`${inputClass} min-h-64 resize-y`}
                    value={values.content}
                    onChange={(e) => setValues((v) => ({ ...v, content: e.target.value }))}
                    aria-invalid={!!errors.content}
                    placeholder="스터디 주제, 일정, 진행 방식, 모집 인원 등을 적어 주세요."
                />
            </FormField>

            <FormError message={errorMessage} />

            <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
                <button type="button" className={secondaryButtonClass} onClick={onCancel} disabled={submitting}>
                    취소
                </button>
                <button type="submit" className={primaryButtonClass} disabled={submitting}>
                    {submitting ? '저장 중...' : submitLabel}
                </button>
            </div>
        </form>
    )
}
