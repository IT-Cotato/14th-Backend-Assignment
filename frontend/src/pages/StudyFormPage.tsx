import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getErrorMessage } from '../api/errors'
import { createStudyPost, getStudyPost, studyPostKeys, updateStudyPost } from '../api/studyPosts'
import { STUDY_STATUS_LABEL, type StudyPostDetail, type StudyStatus } from '../api/types'
import { FormField } from '../components/FormField'
import { ErrorMessage, inputClass, Loading, primaryButtonClass, secondaryButtonClass } from '../components/ui'

const TITLE_MAX = 50
const CONTENT_MAX = 2000

interface FormValues {
  title: string
  content: string
  studyStatus: StudyStatus
}

type FieldErrors = Partial<Record<'title' | 'content', string>>

function validate(values: FormValues): FieldErrors {
  const errors: FieldErrors = {}
  if (!values.title.trim()) errors.title = '제목을 입력해 주세요.'
  else if (values.title.trim().length > TITLE_MAX) errors.title = `제목은 ${TITLE_MAX}자 이하로 입력해 주세요.`
  if (!values.content.trim()) errors.content = '내용을 입력해 주세요.'
  else if (values.content.length > CONTENT_MAX) errors.content = `내용은 ${CONTENT_MAX}자 이하로 입력해 주세요.`
  return errors
}

/** /studies/new 와 /studies/:postId/edit 에서 같이 쓴다. */
export function StudyFormPage() {
  const params = useParams()
  const postId = params.postId ? Number(params.postId) : undefined
  const isEdit = postId !== undefined

  const { data: post, isPending, isError, error } = useQuery({
    queryKey: studyPostKeys.detail(postId ?? 0),
    queryFn: () => getStudyPost(postId!),
    enabled: isEdit,
  })

  if (!isEdit) return <StudyForm />
  if (isPending) return <Loading />
  if (isError) return <ErrorMessage>{getErrorMessage(error)}</ErrorMessage>
  if (!post.isAuthor) return <ErrorMessage>모집글 작성자만 수정할 수 있어요.</ErrorMessage>
  return <StudyForm post={post} />
}

function StudyForm({ post }: { post?: StudyPostDetail }) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const isEdit = post !== undefined
  const [values, setValues] = useState<FormValues>({
    title: post?.title ?? '',
    content: post?.content ?? '',
    studyStatus: post?.studyStatus ?? 'RECRUITING',
  })
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})

  const mutation = useMutation({
    mutationFn: async () => {
      const title = values.title.trim()
      if (post) {
        await updateStudyPost(post.postId, { title, content: values.content, studyStatus: values.studyStatus })
        return post.postId
      }
      return (await createStudyPost({ title, content: values.content })).postId
    },
    onSuccess: async (savedId) => {
      await queryClient.invalidateQueries({ queryKey: studyPostKeys.all })
      navigate(`/studies/${savedId}`, { replace: true })
    },
  })

  const update = (key: keyof FormValues, value: string) => {
    setValues((prev) => ({ ...prev, [key]: value }))
    if (key !== 'studyStatus') setFieldErrors((prev) => ({ ...prev, [key]: undefined }))
  }

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    const errors = validate(values)
    setFieldErrors(errors)
    if (Object.keys(errors).length === 0) mutation.mutate()
  }

  return (
    <section className="mx-auto max-w-2xl">
      <h1 className="mb-6 text-2xl font-bold">{isEdit ? '모집글 수정' : '모집글 작성'}</h1>
      <form onSubmit={handleSubmit} noValidate className="space-y-4 rounded-xl bg-white p-5 shadow-sm sm:p-6">
        {mutation.isError && <ErrorMessage>{getErrorMessage(mutation.error)}</ErrorMessage>}

        <FormField id="title" label="제목" error={fieldErrors.title} hint={`${values.title.length}/${TITLE_MAX}`}>
          <input
            id="title"
            className={inputClass}
            maxLength={TITLE_MAX}
            aria-invalid={fieldErrors.title ? true : undefined}
            aria-describedby={fieldErrors.title ? 'title-error' : undefined}
            value={values.title}
            onChange={(e) => update('title', e.target.value)}
          />
        </FormField>
        <FormField
          id="content"
          label="내용"
          error={fieldErrors.content}
          hint={`${values.content.length}/${CONTENT_MAX}`}
        >
          <textarea
            id="content"
            rows={10}
            className={`${inputClass} resize-y`}
            maxLength={CONTENT_MAX}
            placeholder="스터디 주제, 일정, 진행 방식, 모집 인원 등을 적어 주세요."
            aria-invalid={fieldErrors.content ? true : undefined}
            aria-describedby={fieldErrors.content ? 'content-error' : undefined}
            value={values.content}
            onChange={(e) => update('content', e.target.value)}
          />
        </FormField>
        {isEdit && (
          <FormField id="studyStatus" label="모집 상태">
            <select
              id="studyStatus"
              className={inputClass}
              value={values.studyStatus}
              onChange={(e) => update('studyStatus', e.target.value)}
            >
              {(Object.keys(STUDY_STATUS_LABEL) as StudyStatus[]).map((status) => (
                <option key={status} value={status}>
                  {STUDY_STATUS_LABEL[status]}
                </option>
              ))}
            </select>
          </FormField>
        )}

        <div className="flex flex-wrap justify-end gap-2">
          <Link to={isEdit ? `/studies/${post.postId}` : '/studies'} className={secondaryButtonClass}>
            취소
          </Link>
          <button type="submit" disabled={mutation.isPending} className={primaryButtonClass}>
            {mutation.isPending ? '저장하는 중...' : isEdit ? '수정 완료' : '등록하기'}
          </button>
        </div>
      </form>
    </section>
  )
}
