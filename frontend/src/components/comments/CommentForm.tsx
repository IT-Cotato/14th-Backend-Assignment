import { useState, type FormEvent } from 'react'
import { createComment } from '../../api/comments'
import { getErrorMessage } from '../../api/error'
import { ErrorAlert } from '../ErrorAlert'
import { SubmitButton, TextAreaField } from '../FormField'
import { COMMENT_MAX, validateComment } from './commentRules'

interface CommentFormProps {
  postId: number
  onCreated: (commentId: number, content: string) => void
}

export function CommentForm({ postId, onCreated }: CommentFormProps) {
  const [content, setContent] = useState('')
  const [fieldError, setFieldError] = useState<string>()
  const [serverError, setServerError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    if (loading) return
    const error = validateComment(content)
    setFieldError(error)
    if (error) return

    setServerError(null)
    setLoading(true)
    try {
      const trimmed = content.trim()
      const { commentId } = await createComment(postId, trimmed)
      setContent('')
      onCreated(commentId, trimmed)
    } catch (err) {
      // 예: 쓰는 사이에 글이 마감되면 409 "모집이 마감된 글에는 댓글을 쓸 수 없어요."
      setServerError(getErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <form noValidate onSubmit={handleSubmit} className="space-y-3">
      <ErrorAlert message={serverError} />
      <TextAreaField
        label="댓글 내용"
        name="content"
        placeholder="자기소개나 참여하고 싶은 이유를 남겨 신청해 주세요."
        minHeightClass="min-h-28"
        maxCount={COMMENT_MAX}
        value={content}
        error={fieldError}
        onChange={(e) => {
          setContent(e.target.value)
          setFieldError(undefined)
        }}
      />
      <div className="flex justify-end">
        <div className="w-full sm:w-36">
          <SubmitButton loading={loading} loadingText="등록 중...">
            댓글 등록
          </SubmitButton>
        </div>
      </div>
    </form>
  )
}
