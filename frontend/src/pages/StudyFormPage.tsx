import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getErrorMessage } from '../api/client'
import { createStudyPost, getStudyPost, updateStudyPost } from '../api/studyPosts'
import { STATUS_LABELS, STUDY_STATUSES, type StudyStatus } from '../api/types'
import { Card, ErrorMessage, Field, inputClass, Loading, primaryButton } from '../components/ui'

/** /studies/new (작성), /studies/:id/edit (수정) */
export default function StudyFormPage() {
  const { id } = useParams()
  const postId = id ? Number(id) : null
  const isEdit = postId !== null
  const navigate = useNavigate()

  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [studyStatus, setStudyStatus] = useState<StudyStatus>('RECRUITING')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(isEdit)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (!isEdit) return
    // 늦게 도착한 이전 요청의 응답이 사용자가 고친 입력값을 덮어쓰지 않게 해요
    let ignore = false
    getStudyPost(postId)
      .then((post) => {
        if (ignore) return
        if (!post.isAuthor) {
          setError('모집글 작성자만 수정할 수 있어요.')
          return
        }
        setTitle(post.title)
        setContent(post.content)
        setStudyStatus(post.studyStatus)
      })
      .catch((err) => !ignore && setError(getErrorMessage(err, '모집글을 불러오지 못했어요.')))
      .finally(() => !ignore && setLoading(false))
    return () => {
      ignore = true
    }
  }, [isEdit, postId])

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    if (!title.trim() || title.length > 50) return setError('제목은 1~50자여야 해요.')
    if (!content.trim() || content.length > 2000) return setError('내용은 1~2000자여야 해요.')

    setSubmitting(true)
    try {
      const res = isEdit
        ? await updateStudyPost(postId, { title, content, studyStatus })
        : await createStudyPost({ title, content })
      navigate(`/studies/${res.postId}`)
    } catch (err) {
      setError(getErrorMessage(err, '저장하지 못했어요.'))
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) return <Loading />

  return (
    <div className="mx-auto max-w-2xl">
      <Card>
        <h1 className="mb-6 text-xl font-bold">{isEdit ? '모집글 수정' : '모집글 작성'}</h1>
        <form onSubmit={handleSubmit} noValidate className="space-y-4">
          <Field id="title" label="제목">
            <input id="title" className={inputClass} maxLength={50} value={title} onChange={(e) => setTitle(e.target.value)} />
          </Field>
          <Field id="content" label="내용">
            <textarea id="content" rows={10} className={inputClass} maxLength={2000} value={content} onChange={(e) => setContent(e.target.value)} />
          </Field>
          {isEdit && (
            <Field id="studyStatus" label="모집 상태">
              <select id="studyStatus" className={inputClass} value={studyStatus} onChange={(e) => setStudyStatus(e.target.value as StudyStatus)}>
                {STUDY_STATUSES.map((s) => (
                  <option key={s} value={s}>{STATUS_LABELS[s]}</option>
                ))}
              </select>
            </Field>
          )}
          <ErrorMessage message={error} />
          <button type="submit" className={`${primaryButton} w-full`} disabled={submitting}>
            {submitting ? '저장 중...' : isEdit ? '수정하기' : '작성하기'}
          </button>
        </form>
      </Card>
    </div>
  )
}
