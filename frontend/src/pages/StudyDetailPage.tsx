import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { deleteStudyPost, getStudyPost, updateStudyPost } from '../api/studyPosts.ts'
import { getErrorMessage } from '../api/client.ts'
import { PART_LABELS, type StudyPostDetail } from '../api/types.ts'
import CommentSection from '../components/CommentSection.tsx'
import StatusBadge from '../components/StatusBadge.tsx'

export default function StudyDetailPage() {
  const { postId } = useParams()
  const id = Number(postId)
  const navigate = useNavigate()
  const [post, setPost] = useState<StudyPostDetail | null>(null)
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')

  useEffect(() => {
    let cancelled = false
    getStudyPost(id)
      .then((data) => {
        if (!cancelled) {
          setPost(data)
          setError('')
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(getErrorMessage(err))
        }
      })
    return () => {
      cancelled = true
    }
  }, [id])

  // 모집 중 <-> 모집 마감을 바꾼다
  async function handleToggleStatus() {
    if (!post) {
      return
    }
    setActionError('')
    try {
      await updateStudyPost(id, { studyStatus: post.studyStatus === 'RECRUITING' ? 'CLOSED' : 'RECRUITING' })
      setPost(await getStudyPost(id))
    } catch (err) {
      setActionError(getErrorMessage(err))
    }
  }

  async function handleDelete() {
    if (!window.confirm('이 모집글을 삭제할까요? 달린 댓글도 함께 삭제돼요.')) {
      return
    }
    setActionError('')
    try {
      await deleteStudyPost(id)
      navigate('/studies')
    } catch (err) {
      setActionError(getErrorMessage(err))
    }
  }

  if (error) {
    return (
      <div className="rounded-lg bg-white p-6 shadow-sm">
        <p role="alert" className="text-red-600">{error}</p>
        <Link to="/studies" className="mt-3 inline-block text-sm text-indigo-600 hover:underline">목록으로</Link>
      </div>
    )
  }
  if (!post) {
    return <p className="text-gray-500">불러오는 중...</p>
  }

  return (
    <div className="space-y-4">
      <article className="rounded-lg bg-white p-6 shadow-sm">
        <div className="flex items-center gap-2">
          <StatusBadge status={post.studyStatus} />
          <h1 className="text-xl font-bold">{post.title}</h1>
        </div>
        <p className="mt-2 text-sm text-gray-500">
          {post.author.nickname} · {post.author.generation}기 {PART_LABELS[post.author.part]} ·{' '}
          {post.createdAt.slice(0, 10)}
          {post.updatedAt !== post.createdAt ? ' (수정됨)' : ''}
        </p>
        <p className="mt-4 whitespace-pre-wrap">{post.content}</p>

        {post.isAuthor ? (
          <div className="mt-6 flex flex-wrap gap-2 border-t border-gray-100 pt-4">
            <button type="button" onClick={handleToggleStatus}
              className="rounded border border-gray-300 px-3 py-1.5 text-sm hover:bg-gray-100">
              {post.studyStatus === 'RECRUITING' ? '모집 마감하기' : '모집 다시 열기'}
            </button>
            <Link to={`/studies/${post.postId}/edit`}
              className="rounded border border-gray-300 px-3 py-1.5 text-sm hover:bg-gray-100">
              수정
            </Link>
            <button type="button" onClick={handleDelete}
              className="rounded border border-red-300 px-3 py-1.5 text-sm text-red-600 hover:bg-red-50">
              삭제
            </button>
          </div>
        ) : null}
        {actionError ? <p role="alert" className="mt-3 text-sm text-red-600">{actionError}</p> : null}
      </article>

      <CommentSection postId={post.postId} closed={post.studyStatus === 'CLOSED'} />

      <Link to="/studies" className="inline-block text-sm text-indigo-600 hover:underline">← 목록으로</Link>
    </div>
  )
}
