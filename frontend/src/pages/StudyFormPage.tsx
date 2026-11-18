import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { createStudyPost, getStudyPost, updateStudyPost } from '../api/studyPosts.ts'
import { getErrorMessage } from '../api/client.ts'
import { STATUS_LABELS, type StudyStatus } from '../api/types.ts'
import FormField from '../components/FormField.tsx'

// 글 작성(/studies/new)과 수정(/studies/:postId/edit)에 함께 쓴다
export default function StudyFormPage() {
  const { postId } = useParams()
  const isEdit = postId !== undefined
  const navigate = useNavigate()
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [studyStatus, setStudyStatus] = useState<StudyStatus>('RECRUITING')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(isEdit)
  const [submitting, setSubmitting] = useState(false)

  // 수정 화면이면 기존 내용을 불러온다. 내 글이 아니면 상세 화면으로 돌려보낸다.
  useEffect(() => {
    if (!isEdit) {
      return
    }
    let cancelled = false
    getStudyPost(Number(postId))
      .then((post) => {
        if (cancelled) {
          return
        }
        if (!post.isAuthor) {
          navigate(`/studies/${postId}`, { replace: true })
          return
        }
        setTitle(post.title)
        setContent(post.content)
        setStudyStatus(post.studyStatus)
        setLoading(false)
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(getErrorMessage(err))
          setLoading(false)
        }
      })
    return () => {
      cancelled = true
    }
  }, [isEdit, postId, navigate])

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    if (!title.trim() || !content.trim()) {
      setError('제목과 내용을 입력해 주세요.')
      return
    }
    setSubmitting(true)
    try {
      if (isEdit) {
        await updateStudyPost(Number(postId), { title, content, studyStatus })
        navigate(`/studies/${postId}`)
      } else {
        const created = await createStudyPost({ title, content })
        navigate(`/studies/${created.postId}`)
      }
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return <p className="text-gray-500">불러오는 중...</p>
  }

  return (
    <div className="mx-auto max-w-2xl rounded-lg bg-white p-6 shadow-sm">
      <h1 className="mb-4 text-xl font-bold">{isEdit ? '모집글 수정' : '모집글 작성'}</h1>
      <form onSubmit={handleSubmit} noValidate className="space-y-4">
        <FormField id="title" label="제목" value={title} maxLength={50}
          onChange={(e) => setTitle(e.target.value)} />
        <div>
          <label htmlFor="content" className="mb-1 block text-sm font-medium text-gray-700">내용</label>
          <textarea id="content" value={content} maxLength={2000} rows={10}
            onChange={(e) => setContent(e.target.value)}
            className="w-full rounded border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none" />
          <p className="mt-1 text-right text-xs text-gray-400">{content.length} / 2000</p>
        </div>
        {isEdit ? (
          <div>
            <label htmlFor="studyStatus" className="mb-1 block text-sm font-medium text-gray-700">모집 상태</label>
            <select id="studyStatus" value={studyStatus} onChange={(e) => setStudyStatus(e.target.value as StudyStatus)}
              className="rounded border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none">
              {(Object.keys(STATUS_LABELS) as StudyStatus[]).map((s) => (
                <option key={s} value={s}>{STATUS_LABELS[s]}</option>
              ))}
            </select>
          </div>
        ) : null}
        {error ? <p role="alert" className="text-sm text-red-600">{error}</p> : null}
        <div className="flex gap-2">
          <button type="submit" disabled={submitting}
            className="rounded bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700 disabled:opacity-50">
            {submitting ? '저장 중...' : isEdit ? '수정하기' : '등록하기'}
          </button>
          <button type="button" onClick={() => navigate(-1)}
            className="rounded border border-gray-300 px-4 py-2 hover:bg-gray-100">
            취소
          </button>
        </div>
      </form>
    </div>
  )
}
