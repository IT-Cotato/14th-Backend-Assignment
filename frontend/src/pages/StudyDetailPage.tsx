import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getErrorMessage } from '../api/client'
import { deleteStudyPost, getStudyPost, updateStudyPost } from '../api/studyPosts'
import type { StudyPostDetail } from '../api/types'
import CommentSection from '../components/CommentSection'
import StatusBadge from '../components/StatusBadge'
import { AuthorBadge, Card, dangerButton, ErrorMessage, formatDate, Loading, secondaryButton } from '../components/ui'

export default function StudyDetailPage() {
  const postId = Number(useParams().id)
  const navigate = useNavigate()
  const [post, setPost] = useState<StudyPostDetail | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const load = useCallback(async () => {
    try {
      setPost(await getStudyPost(postId))
    } catch (err) {
      setError(getErrorMessage(err, '모집글을 불러오지 못했어요.'))
    }
  }, [postId])

  useEffect(() => {
    void load()
  }, [load])

  const toggleStatus = async () => {
    if (!post) return
    setBusy(true)
    setActionError(null)
    try {
      await updateStudyPost(post.postId, { studyStatus: post.studyStatus === 'RECRUITING' ? 'CLOSED' : 'RECRUITING' })
      await load()
    } catch (err) {
      setActionError(getErrorMessage(err, '모집 상태를 바꾸지 못했어요.'))
    } finally {
      setBusy(false)
    }
  }

  const handleDelete = async () => {
    if (!post || !window.confirm('모집글을 삭제할까요? 달린 댓글도 함께 삭제돼요.')) return
    setBusy(true)
    setActionError(null)
    try {
      await deleteStudyPost(post.postId)
      navigate('/studies', { replace: true })
    } catch (err) {
      setActionError(getErrorMessage(err, '모집글을 삭제하지 못했어요.'))
      setBusy(false)
    }
  }

  if (error) {
    return (
      <div className="space-y-4">
        <ErrorMessage message={error} />
        <Link to="/studies" className="text-sm text-indigo-600 hover:underline">목록으로</Link>
      </div>
    )
  }
  if (!post) return <Loading />

  return (
    <div className="space-y-6">
      <Link to="/studies" className="text-sm text-indigo-600 hover:underline">← 목록으로</Link>
      <Card>
        <article className="space-y-4">
          <div className="flex items-start justify-between gap-3">
            <h1 className="min-w-0 break-words text-xl font-bold">{post.title}</h1>
            <StatusBadge status={post.studyStatus} />
          </div>
          <div className="flex flex-wrap items-center justify-between gap-2 text-xs text-gray-500">
            <AuthorBadge author={post.author} />
            <span>{formatDate(post.createdAt)}</span>
          </div>
          <p className="whitespace-pre-wrap break-words text-gray-800">{post.content}</p>

          {post.isAuthor && (
            <div className="flex flex-wrap justify-end gap-2 border-t border-gray-100 pt-4">
              <button type="button" className={secondaryButton} disabled={busy} onClick={toggleStatus}>
                {post.studyStatus === 'RECRUITING' ? '모집 마감' : '모집 재개'}
              </button>
              <Link to={`/studies/${post.postId}/edit`} className={secondaryButton}>수정</Link>
              <button type="button" className={dangerButton} disabled={busy} onClick={handleDelete}>삭제</button>
            </div>
          )}
          <ErrorMessage message={actionError} />
        </article>
      </Card>
      <CommentSection postId={post.postId} closed={post.studyStatus === 'CLOSED'} />
    </div>
  )
}
